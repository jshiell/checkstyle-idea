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
     * @param baseDir the project's base directory.
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

        if (cache.containsKey(location)) {
            CachedChecker cachedChecker = cache.get(location);
            if (cachedChecker.isValid()) {
                return cachedChecker.getChecker();
            }
        }

        final Checker checker = createChecker(location, baseDir,
                new ListPropertyResolver(location.getProperties()), classLoader);
        cache.put(location, new CachedChecker(checker));
        return checker;
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
    private Checker createChecker(final ConfigurationLocation location,
                                  final File baseDir,
                                  final PropertyResolver resolver,
                                  final ClassLoader contextClassLoader)
            throws CheckstyleException {

        if (LOG.isDebugEnabled()) {
            // debug information

            LOG.debug("Call to create new checker.");

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

        // This variable needs to be final so that it can be accessed from the
        // inner class, but at the same time we have to be able to set its
        // value. Therefor we use a final array.
        final Object[] threadReturn = new Object[1];

        Thread worker = new Thread() {
            public void run() {
                try {
                    threadReturn[0] = new Checker();

                    if (location != null) {
                        InputStream configurationInputStream = null;

                        try {
                            configurationInputStream = location.resolve();
                            final Configuration config = ConfigurationLoader.loadConfiguration(
                                    configurationInputStream, resolver, true);

                            replaceSupressionFilterPath(config, baseDir);

                            ((Checker) threadReturn[0]).configure(config);

                        } finally {
                            if (configurationInputStream != null) {
                                try {
                                    configurationInputStream.close();
                                } catch (IOException e) {
                                    // ignored
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    threadReturn[0] = e;
                }
            }
        };

        if (contextClassLoader != null) {
            worker.setContextClassLoader(contextClassLoader);
        } else {
            final ClassLoader loader = getClass().getClassLoader();
            worker.setContextClassLoader(loader);
        }

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
        if (threadReturn[0] instanceof CheckstyleException) {
            throw (CheckstyleException) threadReturn[0];

        } else if (threadReturn[0] instanceof Throwable) {
            throw new CheckstyleException("Could not load configuration", (Throwable) threadReturn[0]);
        }

        return (Checker) threadReturn[0];
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
}
