package org.infernus.idea.checkstyle;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.csapi.CheckstyleActions;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.Callable;


/**
 * Makes the Checkstyle tool available to the plugin in the correct version. Registered in {@code plugin.xml}.
 * This must be a project-level service because the Checkstyle version is chosen per project.
 */
public class CheckstyleProjectService {

    private static final Logger LOG = Logger.getInstance(CheckstyleProjectService.class);

    /**
     * mock instance which may be set and used by unit tests
     */
    private static CheckstyleProjectService sMock = null;

    private final Project project;

    private Callable<CheckstyleClassLoader> checkstyleClassLoaderFactory = null;
    private CheckstyleClassLoader checkstyleClassLoader = null;

    private final SortedSet<String> supportedVersions;


    public CheckstyleProjectService(@NotNull final Project project) {
        this.project = project;
        supportedVersions = new VersionListReader().getSupportedVersions();
        final CheckStyleConfiguration pluginConfig = CheckStyleConfiguration.getInstance(project);
        activateCheckstyleVersion(pluginConfig.getCurrentPluginConfig().getCheckstyleVersion(),
                pluginConfig.getCurrentPluginConfig().getThirdPartyClasspath());
    }


    @NotNull
    public SortedSet<String> getSupportedVersions() {
        return supportedVersions;
    }


    public boolean isSupportedVersion(@Nullable final String version) {
        return version != null && supportedVersions.contains(version);
    }


    @NotNull
    public String getDefaultVersion() {
        return VersionListReader.getDefaultVersion(supportedVersions);
    }


    public void activateCheckstyleVersion(@Nullable final String requestedVersion,
                                          @Nullable final List<String> thirdPartyJars) {
        final String version = isSupportedVersion(requestedVersion) ? requestedVersion : getDefaultVersion();
        synchronized (project) {
            checkstyleClassLoaderFactory = new Callable<CheckstyleClassLoader>() {
                @Override
                public CheckstyleClassLoader call() {
                    final List<URL> thirdPartyClassPath = toListOfUrls(thirdPartyJars);
                    return new CheckstyleClassLoader(project, version, thirdPartyClassPath);
                }

                @NotNull
                private List<URL> toListOfUrls(@Nullable final List<String> pThirdPartyJars) {
                    List<URL> result = new ArrayList<>();
                    if (pThirdPartyJars != null) {
                        for (final String absolutePath : pThirdPartyJars) {
                            try {
                                result.add(new File(absolutePath).toURI().toURL());
                            } catch (MalformedURLException e) {
                                LOG.warn("Skipping malformed third party class path entry: " + absolutePath, e);
                            }
                        }
                    }
                    return result;
                }
            };
            checkstyleClassLoader = null;
        }
    }


    public CheckstyleActions getCheckstyleInstance() {
        try {
            synchronized (project) {
                if (checkstyleClassLoader == null) {
                    checkstyleClassLoader = checkstyleClassLoaderFactory.call();
                }
                // Don't worry about caching, class loaders do lots of caching.
                return checkstyleClassLoader.loadCheckstyleImpl();
            }
        } catch (CheckStylePluginException e) {
            throw e;
        } catch (Exception e) {
            throw new CheckStylePluginException("internal error", e);
        }
    }


    public static CheckstyleProjectService getInstance(@NotNull final Project pProject) {
        CheckstyleProjectService result = sMock;
        if (result == null) {
            result = ServiceManager.getService(pProject, CheckstyleProjectService.class);
        }
        return result;
    }


    public static void activateMock4UnitTesting(@Nullable final CheckstyleProjectService pMock) {
        sMock = pMock;
    }
}
