package org.infernus.idea.checkstyle.maven;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeSet;
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
import org.jetbrains.idea.maven.utils.MavenArtifactUtil;
import org.jetbrains.idea.maven.utils.MavenLog;
import org.jetbrains.idea.maven.utils.MavenProcessCanceledException;

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

        updatePluginConfigLocationsFromMavenPlugin(checkstyleMavenPlugin,
            currentPluginConfiguration, mavenProject, pluginConfigurationBuilder, project);

        updatePluginScanScopeFromMavenPlugin(checkstyleMavenPlugin, pluginConfigurationBuilder);

        final var newPluginConfiguration = pluginConfigurationBuilder.build();
        if (!currentPluginConfiguration.equals(newPluginConfiguration)) {
            pluginConfigurationManager.setCurrent(pluginConfigurationBuilder.build(), true);
        }
    }

    private static ConfigurationLocation createConfigurationLocation(final Project project,
        final MavenProject mavenProject, final CheckstyleProjectService checkstyleProjectService,
        final String mavenPluginConfigLocation) {

        String configLocation = null;
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
            if (configLocation != null) {
                configurationType = ConfigurationType.PLUGIN_CLASSPATH;
            }
        }

        if (configurationType == null) {
            throw new CheckStylePluginException(
                "Unable to identify ConfigurationType for configured location: "
                + mavenPluginConfigLocation);
        }

        final var configurationLocationFactory = project.getService(
            ConfigurationLocationFactory.class);
        return configurationLocationFactory.create(project, MAVEN_CONFIG_LOCATION_ID,
            configurationType, configLocation, "Maven Config Location",
            NamedScopeHelper.getDefaultScope(project));
    }

    @Nullable
    private static String createConfigLocationPathForLocalAbsoluteFilePath(
        @NotNull final String mavenConfigLocation) {
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
    private static String createConfigLocationPathForLocalFileUrl(
        @NotNull final String mavenConfigLocation) {
        if (mavenConfigLocation.startsWith("file:/")) {
            try {
                return new File(new URL(mavenConfigLocation).toURI()).getPath();
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    @Nullable
    private static String createConfigLocationPathForLocalRelativeFilePath(
        @NotNull final String mavenConfigLocation, @NotNull final VirtualFile rootDirectory) {
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
    private static String createConfigLocationPathForHttpUrl(
        @NotNull final String mavenConfigLocation) {
        if (isValidHttpUri(mavenConfigLocation)) {
            return mavenConfigLocation;
        }
        return null;
    }

    @Nullable
    private static String createConfigLocationPathForPluginClasspath(
        @NotNull final String mavenConfigLocation, @NotNull final ClassLoader classLoader) {
        final var resource = classLoader.getResource(mavenConfigLocation);
        if (resource != null) {
            return mavenConfigLocation;
        }

        return null;
    }

    private static List<String> createThirdPartyClasspath(final MavenPlugin checkstyleMavenPlugin,
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
            if (CHECKSTYLE_MAVEN_ID.equals(dependency.getGroupId(), dependency.getArtifactId())) {
                return false;
            }

            return true;
        }).map(dependency -> {
            final var dependencyRelativePath = Path.of(
                dependency.getGroupId().replace(".", File.separator), dependency.getArtifactId(),
                dependency.getVersion(),
                dependency.getArtifactId() + "-" + dependency.getVersion() + ".jar");
            final var dependencyPath = mavenProject.getLocalRepository().toPath()
                .resolve(dependencyRelativePath);

            return dependencyPath.toAbsolutePath().toString();
        }).toList();
    }

    @Nullable
    private static VirtualFile getOrDownloadCheckstyleMavenPluginPom(
        @NotNull final MavenPlugin checkstyleMavenPlugin, @NotNull final MavenProject mavenProject,
        @NotNull final Path pomPath, @NotNull final Project project) {
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
    private static MavenId findCheckstyleMavenIdInPom(@NotNull final Project project,
        @NotNull final MavenProject mavenProject,
        @NotNull final MavenPlugin checkstyleMavenPlugin) {
        final var checkstylePluginPomPath = MavenArtifactUtil.getArtifactFile(
            mavenProject.getLocalRepository(), checkstyleMavenPlugin.getMavenId(), "pom");
        final var checkstylePluginVirtualFile = getOrDownloadCheckstyleMavenPluginPom(
            checkstyleMavenPlugin, mavenProject, checkstylePluginPomPath, project);
        if (checkstylePluginVirtualFile == null) {
            return null;
        }

        return ReadAction.compute(() -> {
            final var mavenDomProjectModel = Objects.requireNonNull(
                MavenDomUtil.getMavenDomProjectModel(project, checkstylePluginVirtualFile));
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
    private static MavenProject findMavenProject(
        @NotNull final MavenAfterImportConfigurator.Context context) {
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
            }).sorted(Comparator.comparing(o -> o.getMavenProject().getMavenId().getKey())).findFirst()
            .map(MavenProjectWithModules::getMavenProject).orElse(null);
    }

    private static boolean getChildElementAsBoolean(@Nullable final Element element,
        @NotNull final String childName, final boolean defaultValue) {
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
    private static ScanScope getScanScopeFromMavenConfig(
        @Nullable final Element checkstyleMavenPluginConfig) {
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
        final PluginConfiguration currentPluginConfiguration, final MavenProject mavenProject,
        final PluginConfigurationBuilder pluginConfigurationBuilder, final Project project) {
        final var checkstyleMavenPluginConfiguration = checkstyleMavenPlugin.getConfigurationElement();
        final var configLocations = new TreeSet<>(currentPluginConfiguration.getLocations());
        pluginConfigurationBuilder.withLocations(configLocations);

        final var activeConfigLocationIds = new TreeSet<>(
            currentPluginConfiguration.getActiveLocationIds());
        pluginConfigurationBuilder.withActiveLocationIds(activeConfigLocationIds);

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

        final String mavenPluginConfigLocation = configLocationElement.getText();
        // This must come after the PluginConfigurationBuilder is modified with the new
        // Checkstyle version and the new third party classpath.
        final var tempConfiguration = pluginConfigurationBuilder.build();
        final var checkstyleProjectService = CheckstyleProjectService.forVersion(project,
            tempConfiguration.getCheckstyleVersion(), tempConfiguration.getThirdPartyClasspath());
        final var configurationLocation = createConfigurationLocation(project, mavenProject,
            checkstyleProjectService, mavenPluginConfigLocation);

        configLocations.add(configurationLocation);
        activeConfigLocationIds.add(configurationLocation.getId());
    }

    private static void updatePluginScanScopeFromMavenPlugin(
        final MavenPlugin checkstyleMavenPlugin,
        final PluginConfigurationBuilder pluginConfigurationBuilder) {
        final var checkstyleMavenPluginConfiguration = checkstyleMavenPlugin.getConfigurationElement();
        final var scanScope = getScanScopeFromMavenConfig(checkstyleMavenPluginConfiguration);
        pluginConfigurationBuilder.withScanScope(scanScope);
    }

    // Copy of the private IntelliJ implementation.
    private static class MavenLogEventHandler implements MavenEventHandler {

        @Override
        public void handleConsoleEvents(@NotNull List<? extends MavenServerConsoleEvent> list) {
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
        public void handleDownloadEvents(@NotNull List<? extends MavenArtifactEvent> list) {
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
