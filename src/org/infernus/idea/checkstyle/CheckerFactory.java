package org.infernus.idea.checkstyle;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertyResolver;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.URLClassLoader;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
     */
    private final Map<File, CheckerValue> cache
            = new HashMap<File, CheckerValue>();

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
     * <p/>
     * Use with care: This does not cache the checker.
     *
     * @param configStream a stream for a CheckStyle configuration file.
     * @param properties a list of properties to set. May be null.
     * @param classLoader  class loader for CheckStyle use, or null to use
     *                     the default.
     * @return a checker.
     * @throws CheckstyleException if CheckStyle initialisation fails.
     */
    public Checker getChecker(final InputStream configStream,
                              final ClassLoader classLoader,
                              final Map<String, String> properties)
            throws CheckstyleException {
        if (configStream == null) {
            throw new IllegalArgumentException("Config stream may not be null");
        }

        return createChecker(configStream, null, classLoader, properties);
    }

    /**
     * Get a checker for a given configuration file.
     * <p/>
     * This method operates with the aid of a cache. However, forceReload
     * is provided to allow static scans to force a reload and ensure latest
     * classloader changes are picked up. For the real-time scan, where
     * performance is more of an issue, we can ignore this and use the cached
     * version.
     *
     * @param configFile  the location of the configuration file.
     * @param classLoader class loader for CheckStyle use, or null to use
     *                    the default.
     * @param properties a list of properties to set. May be null.
     * @param forceReload force a reload of the checker rather than using
     *                    a cached value.
     * @return a checker.
     * @throws CheckstyleException if CheckStyle initialisation fails.
     * @throws IOException         if the file cannot be successfully read.
     */
    public Checker getChecker(final File configFile,
                              final ClassLoader classLoader,
                              final Map<String, String> properties,
                              final boolean forceReload)
            throws CheckstyleException, IOException {
        if (configFile == null) {
            throw new IllegalArgumentException("Config file may not be null");
        }

        if (!configFile.exists()) {
            throw new FileNotFoundException("File does not exist: "
                    + configFile);
        }

        final long configFileModified = configFile.lastModified();
        final CheckerValue checkerValue = cache.get(configFile);

        // if not cached or out of date...
        if (forceReload || checkerValue == null
                || checkerValue.getTimeStamp() != configFileModified) {
            final InputStream in = new BufferedInputStream(
                    new FileInputStream(configFile));
            final Checker checker = createChecker(in, configFile.getParentFile(), classLoader, properties);
            in.close();

            cache.put(configFile, new CheckerValue(
                    checker, configFileModified));

            return checker;
        }

        return checkerValue.getChecker();
    }


    /**
     * Create a checker with configuration in a given location.
     *
     * @param location the location of the configuration.
     * @param baseDir the base directory of the configuration file, if available.
     * @param contextClassLoader the context class loader, or null for default.
     * @param properties a list of properties to set. May be null.
     * @return the checker.
     * @throws CheckstyleException if checker initialisation fails.
     */
    private Checker createChecker(final InputStream location,
                                  final File baseDir,
                                  final ClassLoader contextClassLoader,
                                  final Map<String, String> properties)
            throws CheckstyleException {
        return createChecker(location, baseDir, new ListPropertyResolver(properties),
                contextClassLoader);
    }

    /**
     * Load the Checkstyle configuration in a separate thread.
     *
     * @param configPath The path to the Checkstyle configuration file.
     * @param baseDir the base directory of the configuration file, if available.
     * @param resolver   the resolver.
     * @param contextClassLoader the context class loader, or null for default.
     * @return loaded Configuration object
     * @throws CheckstyleException If there was any error loading the configuration file.
     */
    public Checker createChecker(final InputStream configPath,
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

                    if (configPath != null) {
                        final Configuration config
                                = ConfigurationLoader.loadConfiguration(
                                configPath, resolver, true);

                        replaceSupressionFilterPath(config, baseDir);

                        ((Checker) threadReturn[0]).configure(config);
                    }

                } catch (CheckstyleException e) {
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
        }

        return (Checker) threadReturn[0];
    }

    /**
     * Scans the configurtion for supression filters and replaces relative paths with absolute ones.
     *
     * @param config the current configuration.
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

    /**
     * Key for checker cache.
     */
    protected class CheckerValue {

        private Checker checker;
        private long timeStamp;

        /**
         * Create a new checker value.
         *
         * @param checker   the checker instance.
         * @param timeStamp the timestamp of the config file.
         */
        public CheckerValue(final Checker checker, final long timeStamp) {
            if (checker == null) {
                throw new IllegalArgumentException(
                        "Checker may not be null");
            }

            this.checker = checker;
            this.timeStamp = timeStamp;
        }

        /**
         * Get the checker.
         *
         * @return the checker.
         */
        public Checker getChecker() {
            return checker;
        }

        /**
         * Get the timestamp of the config file.
         *
         * @return the timestamp of the config file.
         */
        public long getTimeStamp() {
            return timeStamp;
        }
    }

    /**
     * Property resolver using internal lists.
     */
    protected class ListPropertyResolver implements PropertyResolver {

        private final Map<String, String> propertyNamesToValues
                = new HashMap<String, String>();

        private final List<String> propertyNames = new ArrayList<String>();

        /**
         * Create a default property resolver.
         */
        public ListPropertyResolver() {
        }

        /**
         * Create a property resolver with the given properties.
         *
         * @param properties the properties to make available.
         */
        public ListPropertyResolver(final Map<String, String> properties) {
            setProperties(properties);
        }

        /**
         * Get the list of property names.
         *
         * @return the list of property names.
         */
        public List<String> getPropertyNames() {
            return propertyNames;
        }

        /**
         * Get the map of property names to values.
         *
         * @return the map of property names to values.
         */
        public Map<String, String> getPropertyNamesToValues() {
            return propertyNamesToValues;
        }

        /**
         * {@inheritDoc}
         */
        public String resolve(final String propertyName) throws CheckstyleException {
            // collect properties that are referenced in the config file
            if (!propertyNames.contains(propertyName)) {
                propertyNames.add(propertyName);

                propertyNamesToValues.put(propertyName, "Property '" + propertyName
                        + "' has no value defined in the configuration.");
            }

            return propertyNamesToValues.get(propertyName);
        }

        /**
         * Get the number of properties.
         *
         * @return the number of properties.
         */
        public int getPropertyCount() {
            return propertyNames.size();
        }

        /**
         * Get the property name at the given index.
         *
         * @param index the index.
         * @return the property name at that index.
         */
        public String getPropertyName(final int index) {
            return propertyNames.get(index);
        }

        /**
         * Get the property value at the given index.
         *
         * @param index the index.
         * @return the property value at the index.
         */
        public String getPropertyValue(final int index) {
            return propertyNamesToValues.get(propertyNames.get(index));
        }

        /**
         * Add or update the property with the given name.
         *
         * @param name  the name of the property.
         * @param value the value of the property.
         */
        public void setProperty(final String name, final String value) {
            if (!propertyNames.contains(name)) {
                propertyNames.add(name);
            }

            propertyNamesToValues.put(name, value);
        }

        /**
         * Set all the properties in the map.
         *
         * @param properties a map of properties to set.
         */
        public void setProperties(final Map<String, String> properties) {
            if (properties == null) {
                return;
            }

            for (final String propertyName : properties.keySet()) {
                setProperty(propertyName, properties.get(propertyName));
            }
        }
    }
}
