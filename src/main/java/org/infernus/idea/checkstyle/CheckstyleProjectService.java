package org.infernus.idea.checkstyle;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.checker.ClasspathStabilizer;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.csapi.CheckstyleActions;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.exception.CheckstyleDownloadException;
import org.infernus.idea.checkstyle.util.TempDirProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.concurrent.Callable;


/**
 * Makes the Checkstyle tool available to the plugin in the correct version. Registered in {@code plugin.xml}.
 * This must be a project-level service because the Checkstyle version is chosen per project.
 */
public class CheckstyleProjectService {

    private static final Logger LOG = Logger.getInstance(CheckstyleProjectService.class);

    private final Project project;
    private final Object lock = new Object();

    private Callable<CheckstyleClassLoaderContainer> checkstyleClassLoaderFactory = null;
    private CheckstyleClassLoaderContainer checkstyleClassLoaderContainer = null;

    private final VersionListReader versionListReader;
    private final SortedSet<String> supportedVersions;
    private final CheckstyleArtifactDownloader downloader;
    private final TempDirProvider tempDirProvider;

    public CheckstyleProjectService(@NotNull final Project project) {
        this(project, pluginConfigurationManager(project).getCurrent().getCheckstyleVersion(),
                pluginConfigurationManager(project).getCurrent().getThirdPartyClasspath(),
                CheckstyleArtifactDownloader.create(CheckstyleArtifactDownloader.defaultM2Root()),
                new TempDirProvider());
    }

    CheckstyleProjectService(@NotNull final Project project,
                             @NotNull final CheckstyleArtifactDownloader downloader) {
        this(project, pluginConfigurationManager(project).getCurrent().getCheckstyleVersion(),
                pluginConfigurationManager(project).getCurrent().getThirdPartyClasspath(),
                downloader, new TempDirProvider());
    }

    CheckstyleProjectService(@NotNull final Project project,
                             @NotNull final TempDirProvider tempDirProvider) {
        this(project, pluginConfigurationManager(project).getCurrent().getCheckstyleVersion(),
                pluginConfigurationManager(project).getCurrent().getThirdPartyClasspath(),
                null, tempDirProvider);
    }

    private CheckstyleProjectService(@NotNull final Project project,
                                     @Nullable final String requestedVersion,
                                     @Nullable final List<String> thirdPartyJars,
                                     @Nullable final CheckstyleArtifactDownloader downloaderOverride,
                                     @NotNull final TempDirProvider tempDirProvider) {
        this.project = project;
        this.tempDirProvider = tempDirProvider;
        versionListReader = new VersionListReader();
        supportedVersions = versionListReader.getSupportedVersions();
        this.downloader = downloaderOverride;

        ensureAValidatingParsingIsSetIfPiccoloIsInClasspath();

        activateCheckstyleVersion(requestedVersion, thirdPartyJars);
    }

    private static void ensureAValidatingParsingIsSetIfPiccoloIsInClasspath() {
        // Piccolo is non-validating, but CS needs a validating parser, so we need to ensure that a validating parser
        // is available if Piccolo is on the project classpath
        try {
            Class.forName("org.apache.xerces.jaxp.SAXParserFactoryImpl");
            System.setProperty("com.bluecast.xml.ValidatingSAXParserFactory", "org.apache.xerces.jaxp.SAXParserFactoryImpl");
        } catch (ClassNotFoundException ignored) {
            // ignored
        }
    }

    @NotNull
    public static CheckstyleProjectService forVersion(@NotNull final Project project,
                                                      @Nullable final String requestedVersion,
                                                      @Nullable final List<String> thirdPartyJars) {
        return new CheckstyleProjectService(project, requestedVersion, thirdPartyJars, null, new TempDirProvider());
    }

    @NotNull
    public static CheckstyleProjectService forVersion(@NotNull final Project project,
                                                      @Nullable final String requestedVersion,
                                                      @Nullable final List<String> thirdPartyJars,
                                                      @Nullable final CheckstyleArtifactDownloader downloader) {
        return new CheckstyleProjectService(project, requestedVersion, thirdPartyJars, downloader, new TempDirProvider());
    }

    @Nullable
    public CheckstyleArtifactDownloader getDownloader() {
        return downloader;
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
        boolean isBundled = versionListReader.isBundled(checkstyleVersionToLoad);
        boolean copyLibs = pluginConfigurationManager(project).getCurrent().isCopyLibs();
        synchronized (lock) {
            checkstyleClassLoaderContainer = null;
            checkstyleClassLoaderFactory = () -> {
                List<URL> thirdPartyUrls = resolveThirdPartyUrls(thirdPartyJars, copyLibs);
                if (isBundled) {
                    return new CheckstyleClassLoaderContainer(
                            project, this, checkstyleVersionToLoad, thirdPartyUrls);
                } else {
                    if (downloader == null) {
                        throw new CheckStylePluginException(
                                "Checkstyle " + checkstyleVersionToLoad + " is not bundled and has not been downloaded");
                    }
                    try {
                        return new CheckstyleClassLoaderContainer(
                                project, this, downloader.download(checkstyleVersionToLoad), thirdPartyUrls);
                    } catch (CheckstyleDownloadException e) {
                        throw new CheckStylePluginException(
                                "Failed to download Checkstyle " + checkstyleVersionToLoad + ": " + e.getMessage(), e);
                    }
                }
            };
        }
    }

    @NotNull
    private List<URL> resolveThirdPartyUrls(@Nullable final List<String> jarFilePaths, final boolean copyLibs) {
        List<URL> urls = toListOfUrls(jarFilePaths);
        if (copyLibs && !urls.isEmpty()) {
            Optional<File> copyDir = tempDirProvider.forCopiedLibraries(project);
            if (copyDir.isPresent()) {
                return Arrays.asList(new ClasspathStabilizer(project, copyDir.get().toPath()).stabilize(urls));
            }
        }
        return urls;
    }

    @NotNull
    private List<URL> toListOfUrls(@Nullable final List<String> jarFilePaths) {
        List<URL> result = new ArrayList<>();
        if (jarFilePaths != null) {
            for (final String absolutePath : jarFilePaths) {
                try {
                    result.add(new File(absolutePath).toURI().toURL());
                } catch (MalformedURLException e) {
                    LOG.warn("Skipping malformed third party classpath entry: " + absolutePath, e);
                }
            }
        }
        return result;
    }

    @NotNull
    private String versionToLoad(@Nullable final String requestedVersion) {
        if (requestedVersion != null && versionListReader.isLatest(requestedVersion)) {
            return getDefaultVersion();
        }
        if (requestedVersion != null && supportedVersions.contains(requestedVersion)) {
            return requestedVersion;
        }
        return getDefaultVersion();
    }

    public CheckstyleActions getCheckstyleInstance() {
        try {
            synchronized (lock) {
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
            synchronized (lock) {
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
