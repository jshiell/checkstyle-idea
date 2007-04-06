package org.infernus.idea.checkstyle;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertyResolver;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A configuration factory and resolver for CheckStyle.
 *
 * @author James Shiell
 * @version 1.1
 */
public class CheckerFactory {

    /**
     * A singleton instance.
     */
    private static final CheckerFactory INSTANCE = new CheckerFactory();

    /**
     * Cached checkers for
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
     * @param classLoader  class loader for CheckStyle use, or null to use
     *                     the default.
     * @return a checker.
     * @throws CheckstyleException if CheckStyle initialisation fails.
     */
    public Checker getChecker(final InputStream configStream,
                              final ClassLoader classLoader)
            throws CheckstyleException {
        if (configStream == null) {
            throw new IllegalArgumentException("Config stream may not be null");
        }

        return createChecker(configStream, classLoader);
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
     * @param forceReload force a reload of the checker rather than using
     *                    a cached value.
     * @return a checker.
     * @throws CheckstyleException if CheckStyle initialisation fails.
     * @throws IOException         if the file cannot be successfully read.
     */
    public Checker getChecker(final File configFile,
                              final ClassLoader classLoader,
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
            final Checker checker = createChecker(in, classLoader);
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
     * @param contextClassLoader the context class loader, or null for default.
     * @return the checker.
     * @throws CheckstyleException if checker initialisation fails.
     */
    private Checker createChecker(final InputStream location,
                                  final ClassLoader contextClassLoader)
            throws CheckstyleException {
        return createChecker(location, new ListPropertyResolver(),
                contextClassLoader);
    }

    /**
     * Load the Checkstyle configuration in a separate thread.
     *
     * @param configPath The path to the Checkstyle configuration file
     * @param resolver   the resolver
     * @param contextClassLoader the context class loader, or null for default.
     * @return loaded Configuration object
     * @throws CheckstyleException If there was any error loading the configuration file.
     */
    public Checker createChecker(final InputStream configPath,
                                 final PropertyResolver resolver,
                                 final ClassLoader contextClassLoader)
            throws CheckstyleException {

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
                        ((Checker) threadReturn[0]).configure(config);
                    }

                } catch (CheckstyleException e) {
                    threadReturn[0] = e;
                }
            }
        };

        // Fetch the class loader from the JetStyle plugin
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
    }
}
