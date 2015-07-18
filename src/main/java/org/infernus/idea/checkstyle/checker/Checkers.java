package org.infernus.idea.checkstyle.checker;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.puppycrawl.tools.checkstyle.PropertyResolver;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.CheckStyleConfiguration;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.util.Notifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;

public class Checkers {

    private static final Log LOG = LogFactory.getLog(Checkers.class);

    private final CheckerFactoryCache cache;

    public Checkers(@NotNull final CheckerFactoryCache cache) {
        this.cache = cache;
    }

    public void verify(final ConfigurationLocation location, final List<String> thirdPartyJars)
            throws CheckstyleException, IOException {
        getChecker(null, null, location, classLoaderForPaths(toFileUrls(thirdPartyJars)));
    }

    public Map<PsiFile, List<ProblemDescriptor>> scan(@NotNull final Project project,
                                                      @Nullable final Module module,
                                                      @NotNull final List<ScannableFile> scannableFiles,
                                                      @Nullable final ConfigurationLocation configurationLocation,
                                                      @NotNull final CheckStyleConfiguration pluginConfiguration,
                                                      @Nullable final ClassLoader moduleClassLoader,
                                                      final boolean useExtendedDescriptors) {
        if (scannableFiles.isEmpty()) {
            return emptyMap();
        }

        final CheckStyleChecker checkStyleChecker = getChecker(project, module, configurationLocation, moduleClassLoader);
        final Configuration config = getConfig(project, module, configurationLocation);
        if (checkStyleChecker == null || config == null) {
            return emptyMap();
        }

        return checkStyleChecker.process(scannableFiles, inspectionManager(module), useExtendedDescriptors,
                pluginConfiguration, config);
    }

    private InspectionManager inspectionManager(final Module module) {
        return InspectionManager.getInstance(module.getProject());
    }

    @Nullable
    private CheckStyleChecker getChecker(@Nullable final Project project,
                                         @Nullable final Module module,
                                         @Nullable final ConfigurationLocation location,
                                         @Nullable final ClassLoader classLoader) {
        LOG.debug("Getting CheckStyle checker with location " + location);

        if (location == null) {
            return null;
        }

        try {
            final CachedChecker cachedChecker = getOrCreateCachedChecker(location, project, module, classLoader);
            if (cachedChecker != null) {
                return cachedChecker.getCheckStyleChecker();
            }
            return null;

        } catch (Exception e) {
            throw new CheckStylePluginException("Couldn't create Checker from " + location, e);
        }
    }

    private CachedChecker getOrCreateCachedChecker(final ConfigurationLocation location,
                                                   final Project project,
                                                   final Module module,
                                                   final ClassLoader classLoader)
            throws IOException, CheckstyleException {
        final Optional<CachedChecker> cachedChecker = cache.get(location);
        if (cachedChecker.isPresent()) {
            return cachedChecker.get();
        }

        final ListPropertyResolver propertyResolver = new ListPropertyResolver(location.getProperties());
        final CachedChecker checker = createChecker(location, module, propertyResolver,
                classLoaderFor(project, module, classLoader));
        if (checker != null) {
            cache.put(location, checker);
            return checker;
        }

        return null;
    }

    private ClassLoader classLoaderFor(final Project project, final Module module, final ClassLoader overrideClassLoader)
            throws MalformedURLException {
        if (overrideClassLoader == null && project != null) {
            return moduleClassPathBuilder(project).build(module);
        }
        return overrideClassLoader;
    }

    private ClassLoader classLoaderForPaths(final List<URL> classPaths) {
        final URL[] urls = new URL[classPaths.size()];
        return new URLClassLoader(classPaths.toArray(urls), this.getClass().getClassLoader());
    }

    private List<URL> toFileUrls(final List<String> thirdPartyJars) throws MalformedURLException {
        final List<URL> thirdPartyClassPath = new ArrayList<>();
        for (String path : thirdPartyJars) {
            thirdPartyClassPath.add(new File(path).toURI().toURL());
        }
        return thirdPartyClassPath;
    }

    private ModuleClassPathBuilder moduleClassPathBuilder(final Project project) {
        return ServiceManager.getService(project, ModuleClassPathBuilder.class);
    }

    @Nullable
    private Configuration getConfig(@NotNull final Project project,
                                    @Nullable final Module module,
                                    @Nullable final ConfigurationLocation location) {
        LOG.debug("Getting CheckStyle config for location " + location);

        try {
            final CachedChecker checker = getOrCreateCachedChecker(location, project, module, null);
            if (checker == null) {
                return null;
            }
            return checker.getConfig();

        } catch (Exception e) {
            LOG.error("Checker could not be created.", e);
            throw new CheckStylePluginException("Couldn't create Checker from " + location, e);
        }
    }

    private CachedChecker createChecker(final ConfigurationLocation location,
                                        final Module module,
                                        final PropertyResolver resolver,
                                        final ClassLoader contextClassLoader)
            throws CheckstyleException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Call to create new checker.");
            logProperties(resolver);
            logClassLoaders(contextClassLoader);
        }

        final Object workerResult = executeWorker(location, module, resolver, contextClassLoader);

        if (workerResult instanceof CheckstyleException) {
            final CheckstyleException checkstyleException = (CheckstyleException) workerResult;
            if (checkstyleException.getMessage().contains("Unable to instantiate DoubleCheckedLocking")) {
                return blacklistAndShowMessage(location, module, "checkstyle.double-checked-locking"
                );
            }
            return blacklistAndShowMessage(location, module, "checkstyle.checker-failed",
                    checkstyleException.getMessage());

        } else if (workerResult instanceof IOException) {
            LOG.info("CheckStyle configuration could not be loaded: " + location.getLocation(),
                    (IOException) workerResult);
            return blacklistAndShowMessage(location, module, "checkstyle.file-not-found", location.getLocation());

        } else if (workerResult instanceof Throwable) {
            location.blacklist();
            throw new CheckstyleException("Could not load configuration", (Throwable) workerResult);
        }

        return (CachedChecker) workerResult;
    }

    private Object executeWorker(final ConfigurationLocation location,
                                 final Module module,
                                 final PropertyResolver resolver,
                                 final ClassLoader contextClassLoader) {
        final CheckerFactoryWorker worker = new CheckerFactoryWorker(
                location, resolver, module, contextClassLoader);
        worker.start();

        while (worker.isAlive()) {
            try {
                worker.join();
            } catch (InterruptedException ignored) {
            }
        }

        return worker.getResult();
    }

    private CachedChecker blacklistAndShowMessage(final ConfigurationLocation location,
                                                  final Module module,
                                                  final String messageKey,
                                                  final Object... messageArgs) {
        if (!location.isBlacklisted()) {
            location.blacklist();

            if (module != null) {
                Notifications.showError(module.getProject(), CheckStyleBundle.message(messageKey, messageArgs));
            } else {
                throw new CheckStylePluginException(CheckStyleBundle.message(messageKey, messageArgs));
            }
        }
        return null;
    }

    private void logClassLoaders(final ClassLoader contextClassLoader) {
        if (contextClassLoader != null) {
            ClassLoader currentLoader = contextClassLoader;
            while (currentLoader != null) {
                if (currentLoader instanceof URLClassLoader) {
                    LOG.debug("+ URLClassLoader: " + currentLoader.getClass().getName());
                    final URLClassLoader urlLoader = (URLClassLoader) currentLoader;
                    for (final URL url : urlLoader.getURLs()) {
                        LOG.debug(" + URL: " + url);
                    }
                } else {
                    LOG.debug("+ ClassLoader: " + currentLoader.getClass().getName());
                }

                currentLoader = currentLoader.getParent();
            }
        }
    }

    private void logProperties(final PropertyResolver resolver) {
        if (resolver != null && resolver instanceof ListPropertyResolver) {
            final ListPropertyResolver listResolver = (ListPropertyResolver) resolver;
            final Map<String, String> propertiesToValues = listResolver.getPropertyNamesToValues();
            for (final String propertyName : propertiesToValues.keySet()) {
                final String propertyValue = propertiesToValues.get(propertyName);
                LOG.debug("- Property: " + propertyName + "=" + propertyValue);
            }
        }
    }

}
