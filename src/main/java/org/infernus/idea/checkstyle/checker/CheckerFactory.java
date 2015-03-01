package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.puppycrawl.tools.checkstyle.PropertyResolver;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.util.IDEAUtilities;
import org.infernus.idea.checkstyle.util.ModuleClassPathBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A configuration factory and resolver for CheckStyle.
 */
public class CheckerFactory {

    private static final Log LOG = LogFactory.getLog(CheckerFactory.class);

    private final Map<ConfigurationLocation, CachedChecker> cache = new HashMap<ConfigurationLocation, CachedChecker>();

    public CheckerContainer getChecker(final ConfigurationLocation location, final List<String> thirdPartyJars)
            throws CheckstyleException, IOException {
        return getChecker(location, null, null, classLoaderForPaths(toFileUrls(thirdPartyJars)));
    }

    /**
     * Get a checker for a given configuration, with the default module classloader.
     *
     * @param location the location of the CheckStyle file.
     * @param project  the current project.
     * @param module   the current module.
     * @return the checker for the module or null if it cannot be created.
     * @throws IOException         if the CheckStyle file cannot be resolved.
     * @throws CheckstyleException if CheckStyle initialisation fails.
     */
    public CheckerContainer getChecker(@NotNull final ConfigurationLocation location,
                                       @Nullable final Project project,
                                       @Nullable final Module module)
            throws CheckstyleException, IOException {
        return getChecker(location, project, module, null);
    }

    /**
     * Get a checker for a given configuration.
     *
     * @param location    the location of the CheckStyle file.
     * @param module      the current module.
     * @param classLoader class loader for CheckStyle use, or null to create a module class-loader if required.
     * @return the checker for the module or null if it cannot be created.
     * @throws IOException         if the CheckStyle file cannot be resolved.
     * @throws CheckstyleException if CheckStyle initialisation fails.
     */
    public CheckerContainer getChecker(@NotNull final ConfigurationLocation location,
                                       @Nullable final Project project,
                                       @Nullable final Module module,
                                       @Nullable final ClassLoader classLoader)
            throws CheckstyleException, IOException {
        final CachedChecker cachedChecker = getOrCreateCachedChecker(location, project, module, classLoader);
        if (cachedChecker != null) {
            return cachedChecker.getCheckerContainer();
        }
        return null;
    }

    private CachedChecker getOrCreateCachedChecker(final ConfigurationLocation location,
                                                   final Project project,
                                                   final Module module,
                                                   final ClassLoader classLoader)
            throws IOException, CheckstyleException {
        synchronized (cache) {
            if (cache.containsKey(location)) {
                final CachedChecker cachedChecker = cache.get(location);
                if (cachedChecker != null && cachedChecker.isValid()) {
                    return cachedChecker;
                } else {
                    if (cachedChecker != null) {
                        cachedChecker.getCheckerContainer().destroy();
                    }
                    cache.remove(location);
                }
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
    }

    private ClassLoader classLoaderFor(final Project project, final Module module, final ClassLoader overrideClassLoader)
            throws MalformedURLException {
        if (overrideClassLoader == null && project != null) {
            return moduleClassPathBuilder(project).build(module);
        }
        return overrideClassLoader;
    }

    private ClassLoader classLoaderForPaths(final List<URL> classPaths) {
        URL[] urls = new URL[classPaths.size()];
        return new URLClassLoader(classPaths.toArray(urls), this.getClass().getClassLoader());
    }

    private List<URL> toFileUrls(final List<String> thirdPartyJars) throws MalformedURLException {
        final List<URL> thirdPartyClassPath = new ArrayList<URL>();
        for (String path : thirdPartyJars) {
            thirdPartyClassPath.add(new File(path).toURI().toURL());
        }
        return thirdPartyClassPath;
    }

    private ModuleClassPathBuilder moduleClassPathBuilder(final Project project) {
        return ServiceManager.getService(project, ModuleClassPathBuilder.class);
    }

    /**
     * Invalidate any cached checkers.
     */
    public void invalidateCache() {
        synchronized (cache) {
            for (CachedChecker cachedChecker : cache.values()) {
                cachedChecker.getCheckerContainer().destroy();
            }
            cache.clear();
        }
    }

    /**
     * Get the checker configuration for a given configuration.
     *
     * @param location the location of the CheckStyle file.
     * @param project  the current project.
     * @param module   the current module.  @return a configuration.
     * @throws IllegalArgumentException if no checker with the given location exists and it cannot be created.
     */
    public Configuration getConfig(@NotNull final ConfigurationLocation location,
                                   @NotNull final Project project,
                                   @Nullable final Module module) {
        try {
            final CachedChecker checker = getOrCreateCachedChecker(location, project, module, null);
            if (checker != null) {
                return checker.getConfig();
            }
            return null;

        } catch (Exception e) {
            throw new IllegalStateException("Unable to find or create a checker from " + location, e);
        }
    }

    /**
     * Load the Checkstyle configuration in a separate thread.
     *
     * @param location           The location of the Checkstyle configuration file.
     * @param module             the current module.
     * @param resolver           the resolver.
     * @param contextClassLoader the context class loader, or null for default.
     * @return loaded Configuration object
     * @throws CheckstyleException If there was any error loading the configuration file.
     */
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
                return blacklistAndShowMessage(location, module, "checkstyle.double-checked-locking",
                        "Not compatible with CheckStyle 5.6+. Remove DoubleCheckedLocking.");
            }
            return blacklistAndShowMessage(location, module, "checkstyle.checker-failed", "Load failed due to {0}",
                    checkstyleException.getMessage());

        } else if (workerResult instanceof IOException) {
            LOG.info("CheckStyle configuration could not be loaded: " + location.getLocation(),
                    (IOException) workerResult);
            return blacklistAndShowMessage(location, module, "checkstyle.file-not-found", "Not found: {0}", location.getLocation());

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
                                                  final String messageFallback,
                                                  final Object... messageArgs) {
        if (!location.isBlacklisted()) {
            location.blacklist();

            final MessageFormat messageFormat = new MessageFormat(IDEAUtilities.getResource(messageKey, messageFallback));
            if (module != null) {
                IDEAUtilities.showError(module.getProject(), messageFormat.format(messageArgs));
            } else {
                throw new CheckStylePluginException(messageFormat.format(messageArgs));
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
