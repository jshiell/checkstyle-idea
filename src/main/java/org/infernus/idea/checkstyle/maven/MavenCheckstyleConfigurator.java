package org.infernus.idea.checkstyle.maven;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.StreamSupport;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.config.PluginConfiguration;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.buildtool.MavenEventHandler;
import org.jetbrains.idea.maven.dom.MavenDomUtil;
import org.jetbrains.idea.maven.dom.MavenPropertyResolver;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;
import org.jetbrains.idea.maven.importing.MavenAfterImportConfigurator;
import org.jetbrains.idea.maven.importing.MavenWorkspaceConfigurator.MavenProjectWithModules;
import org.jetbrains.idea.maven.model.MavenArtifactInfo;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.model.MavenPlugin;
import org.jetbrains.idea.maven.project.MavenEmbeddersManager;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.server.MavenArtifactEvent;
import org.jetbrains.idea.maven.server.MavenArtifactResolutionRequest;
import org.jetbrains.idea.maven.server.MavenServerConsoleEvent;
import org.jetbrains.idea.maven.server.MavenServerConsoleIndicator;
import org.jetbrains.idea.maven.utils.MavenLog;
import org.jetbrains.idea.maven.utils.MavenProcessCanceledException;
import org.jetbrains.idea.maven.utils.MavenUtil;

/**
 * Importer to automatically configure the Checkstyle IntelliJ plugin settings based on the
 * Checkstyle Maven plugin configuration.
 *
 * <p>Only configures project settings at this time and does not modify module settings.
 */
@SuppressWarnings("UnstableApiUsage")
public class MavenCheckstyleConfigurator implements MavenAfterImportConfigurator {
    private static final Logger LOG = Logger.getInstance(MavenCheckstyleConfigurator.class);

    private static final MavenId CHECKSTYLE_MAVEN_ID = new MavenId("com.puppycrawl.tools",
        "checkstyle", null);
    private static final MavenId MAVEN_CHECKSTYLE_PLUGIN_MAVEN_ID = new MavenId(
        "org.apache.maven.plugins", "maven-checkstyle-plugin", null);
    // Reserved ID — must not be used as a user-defined location ID.
    // On every Maven sync any location with this ID is replaced unconditionally.
    private static final String MAVEN_CONFIG_LOCATION_ID = "maven-config-location";

    @Override
    public void afterImport(@NotNull final MavenAfterImportConfigurator.Context context) {
        final var project = context.getProject();
        final var pluginConfigurationManager = project.getService(PluginConfigurationManager.class);
        final var currentPluginConfiguration = pluginConfigurationManager.getCurrent();

        // Require users to opt in to avoid a breaking change.
        if (!currentPluginConfiguration.isImportSettingsFromMaven()) {
            LOG.debug("Maven settings import is disabled");
            return;
        }

        final var mavenProject = findMavenProject(context);
        if (mavenProject == null) {
            LOG.warn("Unable to find Maven project");
            return;
        }

        final var checkstyleMavenPlugin = mavenProject.findPlugin(
            MAVEN_CHECKSTYLE_PLUGIN_MAVEN_ID.getGroupId(),
            MAVEN_CHECKSTYLE_PLUGIN_MAVEN_ID.getArtifactId());
        if (checkstyleMavenPlugin == null) {
            LOG.debug("Maven project does not have the Checkstyle plugin configured");
            return;
        }

        final var checkstyleDependencyMavenId = findCheckstyleMavenId(checkstyleMavenPlugin,
            mavenProject, project);

        final var pluginConfigurationBuilder = PluginConfigurationBuilder.from(
            currentPluginConfiguration);
        if (checkstyleDependencyMavenId != null
            && checkstyleDependencyMavenId.getVersion() != null) {
            pluginConfigurationBuilder.withCheckstyleVersion(
                checkstyleDependencyMavenId.getVersion());
        }

        pluginConfigurationBuilder.withThirdPartyClassPath(
            createThirdPartyClasspath(checkstyleMavenPlugin, mavenProject));

        final var mavenDomProjectModel = ReadAction.compute(() ->
            MavenDomUtil.getMavenDomProjectModel(project, mavenProject.getFile()));

        updatePluginConfigLocationsFromMavenPlugin(checkstyleMavenPlugin,
            currentPluginConfiguration, mavenProject, pluginConfigurationBuilder, project,
            mavenDomProjectModel);

        updatePluginScanScopeFromMavenPlugin(checkstyleMavenPlugin, pluginConfigurationBuilder);

        final var newPluginConfiguration = pluginConfigurationBuilder.build();
        if (currentPluginConfiguration.hasChangedFrom(newPluginConfiguration)) {
            pluginConfigurationManager.setCurrent(newPluginConfiguration, true);
        }
    }

    @Nullable
    private static ConfigurationLocation createConfigurationLocation(
            final Project project,
            final MavenProject mavenProject,
            final CheckstyleProjectService checkstyleProjectService,
            final String mavenPluginConfigLocation) {

        String configLocation;
        ConfigurationType configurationType = null;

        configLocation = createConfigLocationPathForLocalFileUrl(mavenPluginConfigLocation);
        if (configLocation != null) {
            configurationType = ConfigurationType.LOCAL_FILE;
        }

        if (configLocation == null) {
            configLocation = createConfigLocationPathForLocalAbsoluteFilePath(
                mavenPluginConfigLocation);
            if (configLocation != null) {
                configurationType = ConfigurationType.LOCAL_FILE;
            }
        }

        if (configLocation == null) {
            configLocation = createConfigLocationPathForLocalRelativeFilePath(
                mavenPluginConfigLocation, mavenProject.getDirectoryFile());
            if (configLocation != null) {
                configurationType = ConfigurationType.PROJECT_RELATIVE;
            }
        }

        if (configLocation == null) {
            configLocation = createConfigLocationPathForHttpUrl(mavenPluginConfigLocation);
            if (configLocation != null) {
                configurationType = ConfigurationType.HTTP_URL;
            }
        }

        if (configLocation == null) {
            configLocation = createConfigLocationPathForPluginClasspath(mavenPluginConfigLocation,
                checkstyleProjectService.underlyingClassLoader());
            if (configLocation == null) {
                return null;
            }
            configurationType = ConfigurationType.PLUGIN_CLASSPATH;
        }

        final var configurationLocationFactory = project.getService(
            ConfigurationLocationFactory.class);
        return configurationLocationFactory.create(project, MAVEN_CONFIG_LOCATION_ID,
            configurationType, configLocation, "Maven Config Location",
            NamedScopeHelper.getDefaultScope(project));
    }

    @Nullable
    private static String createConfigLocationPathForLocalAbsoluteFilePath(@NotNull final String mavenConfigLocation) {
        try {
            final Path mavenPluginConfigLocationPath = Path.of(mavenConfigLocation);
            if (mavenPluginConfigLocationPath.isAbsolute() && Files.isReadable(
                mavenPluginConfigLocationPath)) {
                return mavenConfigLocation;
            }
        } catch (final InvalidPathException ignored) {
        }

        return null;
    }

    @Nullable
    private static String createConfigLocationPathForLocalFileUrl(@NotNull final String mavenConfigLocation) {
        if (mavenConfigLocation.startsWith("file:/")) {
            try {
                return new File(URI.create(mavenConfigLocation)).getPath();
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    @Nullable
    private static String createConfigLocationPathForLocalRelativeFilePath(
            @NotNull final String mavenConfigLocation,
            @NotNull final VirtualFile rootDirectory) {
        try {
            final Path mavenPluginConfigLocationPath = rootDirectory.toNioPath()
                .resolve(Path.of(mavenConfigLocation));
            if (Files.isReadable(mavenPluginConfigLocationPath)) {
                return mavenPluginConfigLocationPath.toString();
            }
        } catch (final InvalidPathException ignored) {
        }

        return null;
    }

    @Nullable
    private static String createConfigLocationPathForHttpUrl(@NotNull final String mavenConfigLocation) {
        if (isValidHttpUri(mavenConfigLocation)) {
            return mavenConfigLocation;
        }
        return null;
    }

    @Nullable
    private static String createConfigLocationPathForPluginClasspath(
            @NotNull final String mavenConfigLocation,
            @NotNull final ClassLoader classLoader) {
        if (classLoader.getResource(mavenConfigLocation) == null) {
            return null;
        }
        return mavenConfigLocation;
    }

    private static List<String> createThirdPartyClasspath(
            final MavenPlugin checkstyleMavenPlugin,
            final MavenProject mavenProject) {
        // This does not differentiate between dependencies that are providing rules or anything else.
        // It is possible that a dependency might contribute something that modifies the behavior of Checkstyle causing a problem.
        // The Maven sync does not currently provide a solution or workaround for this.
        // https://github.com/jshiell/checkstyle-idea/pull/671#discussion_r2313823941
        return checkstyleMavenPlugin.getDependencies().stream().filter(dependency -> {
            // Ignore anything that doesn't have all the required parts of the MavenId.
            // The artifact can't be detected without all of these parts.
            if (dependency.getArtifactId() == null || dependency.getGroupId() == null
                || dependency.getVersion() == null) {
                return false;
            }

            // Ignore the checkstyle dependency, we know it isn't a third party jar.
            return !CHECKSTYLE_MAVEN_ID.equals(dependency.getGroupId(), dependency.getArtifactId());

        }).map(dependency -> MavenUtil.getArtifactPath(
                mavenProject.getLocalRepository().toPath(),
                new MavenId(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()),
                "jar", null))
            .filter(Objects::nonNull)
            .map(Path::toString)
            .toList();
    }

    @Nullable
    private static VirtualFile getOrDownloadCheckstyleMavenPluginPom(
            @NotNull final MavenPlugin checkstyleMavenPlugin,
            @NotNull final MavenProject mavenProject,
            @NotNull final Path pomPath,
            @NotNull final Project project) {
        ApplicationManager.getApplication().assertIsNonDispatchThread();
        final var pomVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(pomPath);
        // Pom file may already exist from something such as a previous resolution.
        if (pomVirtualFile != null) {
            return pomVirtualFile;
        }

        try {
            BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (scope, continuation) -> {
                try {
                    // Download the Checkstyle Maven Plugin pom file.
                    new MavenEmbeddersManager(project).execute(mavenProject,
                        MavenEmbeddersManager.FOR_DOWNLOAD, mavenEmbedderWrapper -> {
                            final var requests = List.of(new MavenArtifactResolutionRequest(
                                new MavenArtifactInfo(checkstyleMavenPlugin.getMavenId(), "pom",
                                    ""), mavenProject.getRemoteRepositories()));
                            mavenEmbedderWrapper.resolveArtifacts(requests, null,
                                new MavenLogEventHandler(), continuation);
                        });
                } catch (MavenProcessCanceledException e) {
                    LOG.warn("Downloading Checkstyle Maven Plugin pom file cancelled", e);
                    return null;
                }
                return null;
            });
            LocalFileSystem.getInstance()
                .refreshIoFiles(List.of(pomPath.toFile()), true, false, null);
        } catch (InterruptedException e) {
            LOG.warn("Downloading Checkstyle Maven Plugin pom file interrupted", e);
        }
        return VirtualFileManager.getInstance().findFileByNioPath(pomPath);
    }

    @Nullable
    private static MavenId findCheckstyleMavenId(@NotNull final MavenPlugin checkstyleMavenPlugin,
        @NotNull final MavenProject mavenProject, @NotNull final Project project) {
        return checkstyleMavenPlugin.getDependencies().stream().filter(
            dependency -> CHECKSTYLE_MAVEN_ID.equals(dependency.getGroupId(),
                dependency.getArtifactId())).findFirst().orElseGet(
            () -> findCheckstyleMavenIdInPom(project, mavenProject, checkstyleMavenPlugin));
    }

    @Nullable
    private static MavenId findCheckstyleMavenIdInPom(
            @NotNull final Project project,
            @NotNull final MavenProject mavenProject,
            @NotNull final MavenPlugin checkstyleMavenPlugin) {
        final var checkstylePluginPomPath = MavenUtil.getArtifactPath(
            mavenProject.getLocalRepository().toPath(), checkstyleMavenPlugin.getMavenId(), "pom", null);
        if (checkstylePluginPomPath == null) {
            return null;
        }
        final var checkstylePluginVirtualFile = getOrDownloadCheckstyleMavenPluginPom(
            checkstyleMavenPlugin, mavenProject, checkstylePluginPomPath, project);
        if (checkstylePluginVirtualFile == null) {
            return null;
        }

        return ReadAction.compute(() -> {
            final var mavenDomProjectModel = MavenDomUtil.getMavenDomProjectModel(project, checkstylePluginVirtualFile);
            if (mavenDomProjectModel == null) {
                LOG.warn("Maven DOM project model unavailable for " + checkstylePluginVirtualFile.getPath());
                return null;
            }
            final var checkstyleDependency = mavenDomProjectModel.getDependencies()
                .getDependencies().stream().filter(
                    dependency -> CHECKSTYLE_MAVEN_ID.equals(dependency.getGroupId().getValue(),
                        dependency.getArtifactId().getValue())).findFirst().orElseThrow(
                    () -> {
                        LOG.warn(
                                "Checkstyle dependency could not be found within Maven Checkstyle Plugin. Maven DOM project model %s".formatted(
                                        mavenDomProjectModel.toString()));
                        return new CheckStylePluginException(
                                "Failed to find Checkstyle dependency within the Maven Checkstyle Plugin pom");
                    });

            final var version = MavenPropertyResolver.resolve(
                checkstyleDependency.getVersion().getValue(), mavenDomProjectModel);

            return new MavenId(checkstyleDependency.getGroupId().getValue(),
                checkstyleDependency.getArtifactId().getValue(), version);
        });
    }

    @Nullable
    private static MavenProject findMavenProject(@NotNull final MavenAfterImportConfigurator.Context context) {
        // The first MavenProject (sorted alphabetically by MavenId#getKey()) found with a Maven
        // Checkstyle Plugin is what will be used to load settings for the project.
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(context.getMavenProjectsWithModules().iterator(),
                    Spliterator.ORDERED), false).filter(mavenProjectWithModulesToFilter -> {
                final var mavenProject = mavenProjectWithModulesToFilter.getMavenProject();
                final var checkstyleMavenPlugin = mavenProject.findPlugin(
                    MAVEN_CHECKSTYLE_PLUGIN_MAVEN_ID.getGroupId(),
                    MAVEN_CHECKSTYLE_PLUGIN_MAVEN_ID.getArtifactId());

                return checkstyleMavenPlugin != null;
            }).sorted(Comparator.comparing(o -> o.getMavenProject().getMavenId().getKey()))
                .findFirst()
            .map(MavenProjectWithModules::getMavenProject).orElse(null);
    }

    private static boolean getChildElementAsBoolean(
            @Nullable final Element element,
            @NotNull final String childName,
            final boolean defaultValue) {
        if (element == null) {
            return defaultValue;
        }

        final var child = element.getChild(childName);
        if (child == null) {
            return defaultValue;
        }

        return Boolean.parseBoolean(child.getText());
    }

    @NotNull
    private static ScanScope getScanScopeFromMavenConfig(@Nullable final Element checkstyleMavenPluginConfig) {
        // Default values here match the defaults from Maven Checkstyle plugin 3.6.0.
        final var includeResources = getChildElementAsBoolean(checkstyleMavenPluginConfig,
            "includeResources", true);
        final var includeTestResources = getChildElementAsBoolean(checkstyleMavenPluginConfig,
            "includeTestResources", true);
        final var includeTestSourceDirectory = getChildElementAsBoolean(checkstyleMavenPluginConfig,
            "includeTestSourceDirectory", false);

        if (includeResources && includeTestResources && includeTestSourceDirectory) {
            return ScanScope.AllSourcesWithTests;
        }

        if (includeResources && !includeTestResources && !includeTestSourceDirectory) {
            return ScanScope.AllSources;
        }

        if (!includeResources && !includeTestResources && includeTestSourceDirectory) {
            return ScanScope.JavaOnlyWithTests;
        }

        if (!includeResources && !includeTestResources && !includeTestSourceDirectory) {
            return ScanScope.JavaOnly;
        }

        // Maps four explicit (includeResources, includeTestResources, includeTestSourceDirectory) combinations:
        //   (true,  true,  true ) -> AllSourcesWithTests
        //   (true,  false, false) -> AllSources
        //   (false, false, true ) -> JavaOnlyWithTests
        //   (false, false, false) -> JavaOnly
        // Other combinations have no exact ScanScope analogue and fall back to the default.
        LOG.warn("Unrecognised Maven Checkstyle scope combination: includeResources=%s, includeTestResources=%s, includeTestSourceDirectory=%s — falling back to %s"
            .formatted(includeResources, includeTestResources, includeTestSourceDirectory, ScanScope.getDefaultValue()));
        return ScanScope.getDefaultValue();
    }

    private static boolean isValidHttpUri(@NotNull final String uriString) {
        if (!uriString.startsWith("http://") && !uriString.startsWith("https://")) {
            return false;
        }
        try {
            new URI(uriString);
            return true;
        } catch (URISyntaxException ignored) {
            return false;
        }
    }

    private static void updatePluginConfigLocationsFromMavenPlugin(
            final MavenPlugin checkstyleMavenPlugin,
            final PluginConfiguration currentPluginConfiguration,
            final MavenProject mavenProject,
            final PluginConfigurationBuilder pluginConfigurationBuilder,
            final Project project,
        @Nullable final MavenDomProjectModel mavenDomProjectModel) {
        final var checkstyleMavenPluginConfiguration = checkstyleMavenPlugin.getConfigurationElement();
        final var configLocations = new TreeSet<>(currentPluginConfiguration.getLocations());
        pluginConfigurationBuilder.withLocations(configLocations);

        final var activeConfigLocationIds = new TreeSet<>(
            currentPluginConfiguration.getActiveLocationIds());
        pluginConfigurationBuilder.withActiveLocationIds(activeConfigLocationIds);

        currentPluginConfiguration.getLocations().stream()
            .filter(loc -> MAVEN_CONFIG_LOCATION_ID.equals(loc.getId()))
            .filter(loc -> !"Maven Config Location".equals(loc.getDescription()))
            .forEach(loc -> LOG.warn("Replacing non-Maven-managed location carrying reserved id '"
                + MAVEN_CONFIG_LOCATION_ID + "': " + loc));

        configLocations.removeIf(location -> MAVEN_CONFIG_LOCATION_ID.equals(location.getId()));
        activeConfigLocationIds.removeIf(MAVEN_CONFIG_LOCATION_ID::equals);

        if (checkstyleMavenPluginConfiguration == null) {
            return;
        }

        final var configLocationElement = checkstyleMavenPluginConfiguration.getChild(
            "configLocation");
        if (configLocationElement == null || configLocationElement.getText() == null) {
            return;
        }

        final String rawConfigLocation = configLocationElement.getText();
        final String mavenPluginConfigLocation = mavenDomProjectModel != null
            ? ReadAction.compute(() -> MavenPropertyResolver.resolve(rawConfigLocation, mavenDomProjectModel))
            : rawConfigLocation;
        // This must come after the PluginConfigurationBuilder is modified with the new
        // Checkstyle version and the new third party classpath.
        final var tempConfiguration = pluginConfigurationBuilder.build();
        final var checkstyleProjectService = CheckstyleProjectService.forVersion(project,
            tempConfiguration.getCheckstyleVersion(), tempConfiguration.getThirdPartyClasspath());
        final var configurationLocation = createConfigurationLocation(project, mavenProject,
            checkstyleProjectService, mavenPluginConfigLocation);
        if (configurationLocation == null) {
            LOG.warn("Could not resolve <configLocation>" + mavenPluginConfigLocation
                + "</configLocation> — no matching local file, URL, or classpath resource");
            return;
        }
        updateSuppressionLocation(checkstyleMavenPluginConfiguration, configurationLocation, mavenDomProjectModel);

        configLocations.add(configurationLocation);
        activeConfigLocationIds.add(configurationLocation.getId());
    }

    private static void updateSuppressionLocation(
            final Element checkstyleMavenPluginConfiguration,
            final ConfigurationLocation configurationLocation,
            @Nullable final MavenDomProjectModel mavenDomProjectModel) {
        final String propertyName;
        final Element suppressionProperty = checkstyleMavenPluginConfiguration.getChild("suppressionsFileExpression");
        if (suppressionProperty != null && suppressionProperty.getText() != null) {
            propertyName = suppressionProperty.getText();
        } else {
            propertyName = "checkstyle.suppressions.file";
        }

        final Element suppressionLocation = checkstyleMavenPluginConfiguration.getChild("suppressionsLocation");
        if (suppressionLocation != null && suppressionLocation.getText() != null) {
            final String rawValue = suppressionLocation.getText();
            final String resolvedValue = mavenDomProjectModel != null
                ? ReadAction.compute(() -> MavenPropertyResolver.resolve(rawValue, mavenDomProjectModel))
                : rawValue;
            configurationLocation.setProperties(Map.of(propertyName, resolvedValue));
        }
    }

    private static void updatePluginScanScopeFromMavenPlugin(
            final MavenPlugin checkstyleMavenPlugin,
            final PluginConfigurationBuilder pluginConfigurationBuilder) {
            final var checkstyleMavenPluginConfiguration = checkstyleMavenPlugin.getConfigurationElement();
            final var scanScope = getScanScopeFromMavenConfig(checkstyleMavenPluginConfiguration);
        pluginConfigurationBuilder.withScanScope(scanScope);
    }

    // Copy of the private IntelliJ implementation.
    private static final class MavenLogEventHandler implements MavenEventHandler {

        @Override
        public void handleConsoleEvents(@NotNull final List<? extends MavenServerConsoleEvent> list) {
            for (var e : list) {
                var message = e.getMessage();
                switch (e.getLevel()) {
                    case MavenServerConsoleIndicator.LEVEL_DEBUG -> MavenLog.LOG.debug(message);
                    case MavenServerConsoleIndicator.LEVEL_INFO -> MavenLog.LOG.info(message);
                    default -> MavenLog.LOG.warn(message);
                }
                var throwable = e.getThrowable();
                if (null != throwable) {
                    MavenLog.LOG.warn(throwable);
                }
            }
        }

        @Override
        public void handleDownloadEvents(@NotNull final List<? extends MavenArtifactEvent> list) {
            for (var e : list) {
                final var id = e.getDependencyId();
                switch (e.getArtifactEventType()) {
                    case DOWNLOAD_STARTED ->
                        MavenLog.LOG.debug("Download started: %s".formatted(id));
                    case DOWNLOAD_COMPLETED ->
                        MavenLog.LOG.debug("Download completed: %s".formatted(id));
                    case DOWNLOAD_FAILED -> MavenLog.LOG.debug(
                        "Download failed: %s \n%s \n%s".formatted(id, e.getErrorMessage(),
                            e.getStackTrace()));
                }
            }
        }
    }
}
