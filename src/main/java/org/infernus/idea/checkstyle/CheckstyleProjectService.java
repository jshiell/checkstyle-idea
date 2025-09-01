package org.infernus.idea.checkstyle;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.platform.ide.progress.TasksKt;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.Callable;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.csapi.CheckstyleActions;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Makes the Checkstyle tool available to the plugin in the correct version. Registered in
 * {@code plugin.xml}. This must be a project-level service because the Checkstyle version is chosen
 * per project.
 */
public class CheckstyleProjectService {

    private static final Logger LOG = Logger.getInstance(CheckstyleProjectService.class);

    private final Project project;

    private Callable<CheckstyleClassLoaderContainer> checkstyleClassLoaderFactory = null;
    private CheckstyleClassLoaderContainer checkstyleClassLoaderContainer = null;

    private final SortedSet<String> supportedVersions;

    public CheckstyleProjectService(@NotNull final Project project) {
        this(project, pluginConfigurationManager(project).getCurrent().getCheckstyleVersion(),
            pluginConfigurationManager(project).getCurrent().getThirdPartyClasspath());
    }

    private CheckstyleProjectService(@NotNull final Project project,
        @Nullable final String requestedVersion,
        @Nullable final List<String> thirdPartyJars) {
        this.project = project;
        supportedVersions = new VersionListReader().getSupportedVersions();

        ensureAValidatingParsingIsSetIfPiccoloIsInClasspath();

        activateCheckstyleVersion(requestedVersion, thirdPartyJars);
    }

    private static void ensureAValidatingParsingIsSetIfPiccoloIsInClasspath() {
        // Piccolo is non-validating, but CS needs a validating parser, so we need to ensure that a validating parser
        // is available if Piccolo is on the project classpath
        try {
            Class.forName("org.apache.xerces.jaxp.SAXParserFactoryImpl");
            System.setProperty("com.bluecast.xml.ValidatingSAXParserFactory",
                "org.apache.xerces.jaxp.SAXParserFactoryImpl");
        } catch (ClassNotFoundException ignored) {
            // ignored
        }
    }

    @NotNull
    public static CheckstyleProjectService forVersion(@NotNull final Project project,
        @Nullable final String requestedVersion,
        @Nullable final List<String> thirdPartyJars) {
        return new CheckstyleProjectService(project, requestedVersion, thirdPartyJars);
    }

    @NotNull
    public SortedSet<String> getSupportedVersions() {
        return supportedVersions;
    }

    @NotNull
    private String getDefaultVersion() {
        return VersionListReader.getDefaultVersion(supportedVersions);
    }

    public void activateCheckstyleVersion(@Nullable final String requestedVersion,
        @Nullable final List<String> thirdPartyJars) {
        String checkstyleVersionToLoad = versionToLoad(requestedVersion);
        // TODO: Need to offer an option for users to pre-download library versions.
        // TODO: Need to offer an option for users to download the library through an external tool,
        //  and place it into the cache directory for later use.
        final var checkstyleDownloader = ApplicationManager.getApplication()
            .getService(CheckstyleDownloader.class);
        synchronized (project) {

            TasksKt.runWithModalProgressBlocking(project,
                "Downloading Checkstyle Versions",
                (scope, continuation) -> {
                    try {
                        checkstyleDownloader.downloadVersion(checkstyleVersionToLoad);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }

                    return null;
                });

            final var jarPath = checkstyleDownloader.getArtifactPath(checkstyleVersionToLoad);
            checkstyleClassLoaderContainer = null;
            checkstyleClassLoaderFactory = new Callable<>() {
                @Override
                public CheckstyleClassLoaderContainer call() {
                    return new CheckstyleClassLoaderContainer(
                        project,
                        CheckstyleProjectService.this,
                        jarPath,
                        toListOfUrls(thirdPartyJars));
                }

                @NotNull
                private List<URL> toListOfUrls(@Nullable final List<String> jarFilePaths) {
                    List<URL> result = new ArrayList<>();
                    if (jarFilePaths != null) {
                        for (final String absolutePath : jarFilePaths) {
                            try {
                                result.add(new File(absolutePath).toURI().toURL());
                            } catch (MalformedURLException e) {
                                LOG.warn("Skipping malformed third party classpath entry: "
                                         + absolutePath, e);
                            }
                        }
                    }
                    return result;
                }
            };
        }
    }

    @NotNull
    private String versionToLoad(@Nullable final String requestedVersion) {
        if (requestedVersion != null && supportedVersions.contains(requestedVersion)) {
            return requestedVersion;
        }
        return getDefaultVersion();
    }

    public CheckstyleActions getCheckstyleInstance() {
        try {
            synchronized (project) {
                return checkstyleClassLoaderContainer().loadCheckstyleImpl();
            }
        } catch (CheckStylePluginException e) {
            throw e;
        } catch (Exception e) {
            throw new CheckStylePluginException("Internal error", e);
        }
    }

    @NotNull
    public ClassLoader underlyingClassLoader() {
        try {
            synchronized (project) {
                return checkstyleClassLoaderContainer().getClassLoader();
            }
        } catch (CheckStylePluginException e) {
            throw e;
        } catch (Exception e) {
            throw new CheckStylePluginException("Internal error", e);
        }
    }

    private CheckstyleClassLoaderContainer checkstyleClassLoaderContainer() throws Exception {
        if (checkstyleClassLoaderContainer == null) {
            checkstyleClassLoaderContainer = checkstyleClassLoaderFactory.call();
        }
        // Don't worry about caching, class loaders do lots of caching.
        return this.checkstyleClassLoaderContainer;
    }

    private static PluginConfigurationManager pluginConfigurationManager(final Project project) {
        return project.getService(PluginConfigurationManager.class);
    }
}
