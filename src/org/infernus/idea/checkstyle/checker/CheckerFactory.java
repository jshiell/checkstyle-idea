package org.infernus.idea.checkstyle.checker;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.PropertyResolver;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * A configuration factory and resolver for CheckStyle.
 *
 * @author James Shiell
 * @version 1.1
 */
public class CheckerFactory {

    /**
     * Logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(
            CheckerFactory.class);

    /**
     * A singleton instance.
     */
    private static final CheckerFactory INSTANCE = new CheckerFactory();

    /**
     * Cached checkers for the factory.
     * <p/>
     */
    private final Map<ConfigurationLocation, CachedChecker> cache = new HashMap<ConfigurationLocation, CachedChecker>();

    /**
     * Create a new factory.
     */
    protected CheckerFactory() {
    }

    /**
     * Get an instance of the checker factory.
     *
     * @return a checker factory.
     */
    public static CheckerFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Get a checker for a given configuration.
     *
     * @param location    the location of the CheckStyle file.
     * @param classLoader class loader for CheckStyle use, or null to use
     *                    the default.
     * @param baseDir     the project's base directory.
     * @return a checker.
     * @throws CheckstyleException if CheckStyle initialisation fails.
     */
    public Checker getChecker(final ConfigurationLocation location,
                              final File baseDir,
                              final ClassLoader classLoader)
            throws CheckstyleException {
        if (location == null) {
            throw new IllegalArgumentException("Location is required");
        }

        synchronized (cache) {
            if (cache.containsKey(location)) {
                CachedChecker cachedChecker = cache.get(location);
                if (cachedChecker != null && cachedChecker.isValid()) {
                    return cachedChecker.getChecker();
                } else {
                    if (cachedChecker != null) {
                        cachedChecker.getChecker().destroy();
                    }
                    cache.remove(location);
                }
            }

            final CachedChecker checker = createChecker(location, baseDir,
                    new ListPropertyResolver(location.getProperties()), classLoader);
            cache.put(location, checker);

            return checker.getChecker();
        }
    }

    /**
     * Get the checker configuration for a given configuration.
     *
     * @param location the location of the CheckStyle file.
     * @return a configuration.
     */
    public Configuration getConfig(final ConfigurationLocation location) {
        if (location == null) {
            throw new IllegalArgumentException("Location is required");
        }

        synchronized (cache) {
            if (cache.containsKey(location)) {
                CachedChecker cachedChecker = cache.get(location);
                if (cachedChecker != null && cachedChecker.isValid()) {
                    return cachedChecker.getConfig();
                }
            }
        }

        throw new IllegalArgumentException("Failed to find a configured checker.");
    }

    /**
     * Load the Checkstyle configuration in a separate thread.
     *
     * @param location           The location of the Checkstyle configuration file.
     * @param baseDir            the base directory of the configuration file, if available.
     * @param resolver           the resolver.
     * @param contextClassLoader the context class loader, or null for default.
     * @return loaded Configuration object
     * @throws CheckstyleException If there was any error loading the configuration file.
     */
    private CachedChecker createChecker(final ConfigurationLocation location,
                                        final File baseDir,
                                        final PropertyResolver resolver,
                                        final ClassLoader contextClassLoader)
            throws CheckstyleException {

        if (LOG.isDebugEnabled()) {
            // debug information

            LOG.debug("Call to create new checker.");

            logProperties(resolver);
            logClassLoaders(contextClassLoader);
        }

        final CheckerFactoryWorker worker = new CheckerFactoryWorker(
                location, resolver, baseDir, contextClassLoader);

        // Begin reading the configuration
        worker.start();

        // Wait for configuration thread to complete
        while (worker.isAlive()) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                // Just be silent for now
            }
        }

        // Did the process of reading the configuration fail?
        if (worker.getResult() instanceof CheckstyleException) {
            throw (CheckstyleException) worker.getResult();

        } else if (worker.getResult() instanceof Throwable) {
            throw new CheckstyleException("Could not load configuration",
                    (Throwable) worker.getResult());
        }

        return (CachedChecker) worker.getResult();
    }

    private void logClassLoaders(final ClassLoader contextClassLoader) {
        // Log classloaders, if known
        if (contextClassLoader != null) {
            ClassLoader currentLoader = contextClassLoader;
            while (currentLoader != null) {
                if (currentLoader instanceof URLClassLoader) {
                    LOG.debug("+ URLClassLoader: "
                            + currentLoader.getClass().getName());
                    final URLClassLoader urlLoader = (URLClassLoader)
                            currentLoader;
                    for (final URL url : urlLoader.getURLs()) {
                        LOG.debug(" + URL: " + url);
                    }
                } else {
                    LOG.debug("+ ClassLoader: "
                            + currentLoader.getClass().getName());
                }

                currentLoader = currentLoader.getParent();
            }
        }
    }

    private void logProperties(final PropertyResolver resolver) {
        // Log properties if known
        if (resolver != null && resolver instanceof ListPropertyResolver) {
            final ListPropertyResolver listResolver = (ListPropertyResolver)
                    resolver;
            final Map<String, String> propertiesToValues
                    = listResolver.getPropertyNamesToValues();
            for (final String propertyName : propertiesToValues.keySet()) {
                final String propertyValue
                        = propertiesToValues.get(propertyName);
                LOG.debug("- Property: " + propertyName + "="
                        + propertyValue);
            }
        }
    }

    /**
     * Scans the configurtion for supression filters and replaces relative paths with absolute ones.
     *
     * @param config  the current configuration.
     * @param baseDir the base directory of the configuration file.
     * @throws CheckstyleException if configuration fails.
     */
    private void replaceSupressionFilterPath(final Configuration config,
                                             final File baseDir)
            throws CheckstyleException {
        if (baseDir == null) {
            return;
        }

        for (final Configuration configurationElement : config.getChildren()) {
            if (!"SuppressionFilter".equals(configurationElement.getName())) {
                continue;
            }

            final String suppressionFile = configurationElement.getAttribute("file");
            if (suppressionFile != null && !new File(suppressionFile).exists()
                    && configurationElement instanceof DefaultConfiguration) {
                ((DefaultConfiguration) configurationElement).addAttribute(
                        "file", new File(baseDir, suppressionFile).getAbsolutePath());
            }
        }
    }

    private class CheckerFactoryWorker extends Thread {
        private final Object[] threadReturn = new Object[1];

        private final ConfigurationLocation location;
        private final PropertyResolver resolver;
        private final File baseDir;

        public CheckerFactoryWorker(final ConfigurationLocation location,
                                    final PropertyResolver resolver,
                                    final File baseDir,
                                    final ClassLoader contextClassLoader) {
            this.location = location;
            this.resolver = resolver;
            this.baseDir = baseDir;


            if (contextClassLoader != null) {
                setContextClassLoader(contextClassLoader);
            } else {
                final ClassLoader loader = CheckerFactory.this.getClass().getClassLoader();
                setContextClassLoader(loader);
            }
        }

        public Object getResult() {
            return threadReturn[0];
        }

        public void run() {
            try {
                final Checker checker = new Checker();
                final Configuration config;

                if (location != null) {
                    InputStream configurationInputStream = null;

                    try {
                        configurationInputStream = location.resolve();
                        config = ConfigurationLoader.loadConfiguration(
                                configurationInputStream, resolver, true);

                        replaceSupressionFilterPath(config, baseDir);

                        checker.setModuleClassLoader(Thread.currentThread().getContextClassLoader());
                        checker.configure(config);

                    } finally {
                        if (configurationInputStream != null) {
                            try {
                                configurationInputStream.close();
                            } catch (IOException e) {
                                // ignored
                            }
                        }
                    }
                } else {
                    config = new DefaultConfiguration("checker");
                }
                threadReturn[0] = new CachedChecker(checker, config);

            } catch (Exception e) {
                threadReturn[0] = e;
            }
        }
    }
}
