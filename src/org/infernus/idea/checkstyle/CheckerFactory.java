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
   TODO cache these
 
 * @author James Shiell
 * @version 1.0
 */
public class CheckerFactory implements PropertyResolver {

    /**
     * A singleton instance.
     */
    private static final CheckerFactory INSTANCE = new CheckerFactory();

    private final Map<String, String> propertyNamesToValues = new HashMap<String, String>();
    private final List<String> propertyNames = new ArrayList<String>();

    private Checker checker;

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
     * @param checkstyleConfigurationLocation the location of
     * the configuration file.
     * @throws CheckstyleException if CheckStyle initialisation fails.
     * @return a checker.
     */
    public Checker getChecker(final InputStream checkstyleConfigurationLocation)
            throws CheckstyleException {
        createChecker(checkstyleConfigurationLocation);
        return checker;
    }

    /**
     * Get a checker for a given configuration file.
     *
     * @param configFile the location of the configuration file.
     * @throws CheckstyleException if CheckStyle initialisation fails.
     * @throws IOException if the file cannot be successfully read.
     * @return a checker.
     */
    public Checker getChecker(final File configFile)
            throws CheckstyleException, IOException {
        final InputStream in = new BufferedInputStream(
                new FileInputStream(configFile));
        createChecker(in);
        in.close();

        return checker;
    }

    /**
     * {@inheritDoc}
     */
    public String resolve(final String propertyName) throws CheckstyleException {
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
     * @param name the name of the property.
     * @param value the value of the property.
     */
    public void setProperty(final String name, final String value) {
        if (!propertyNames.contains(name)) {
            propertyNames.add(name);
        }

        propertyNamesToValues.put(name, value);
    }

    /**
     * Create a checker with configuration in a given location.
     *
     * @param location the location of the configuration.
     * @return the checker.
     * @throws CheckstyleException if checker initialisation fails.
     */
    private Checker createChecker(final InputStream location)
            throws CheckstyleException {
        propertyNames.clear();
        propertyNamesToValues.clear();

        // collect properties that are referenced in the config file
        PropertyResolver collector = new PropertyResolver() {

            public String resolve(final String propertyName) throws CheckstyleException {
                if (!propertyNames.contains(propertyName)) {
                    propertyNames.add(propertyName);
                    propertyNamesToValues.put(propertyName,
                            "Property '" + propertyName
                                    + "' has no value defined in the configuration.");
                }

                return CheckerFactory.this.resolve(propertyName);
            }
        };

        if (location != null) {
            // ok load the checkstyle configuration in seperate thread
            return createChecker(location, collector);
        }

        return checker;
    }

    /**
     * Load the Checkstyle configuration in a separate thread.
     *
     * @param configPath The path to the Checkstyle configuration file
     * @param resolver   the resolver
     * @return loaded Configuration object
     * @throws CheckstyleException If there was any error loading the configuration file
     */
    public Checker createChecker(final InputStream configPath,
                                 final PropertyResolver resolver)
            throws CheckstyleException {

        // This variable needs to be final so that it can be accessed from the
        // inner class, but at the same time we have to be able to set its
        // value. Therefor we use a final array.
        final Object[] threadReturn = new Object[1];

        Thread worker = new Thread() {
            public void run() {
                try {
                    final Configuration config = ConfigurationLoader.loadConfiguration(
                            configPath, resolver, true);
                    threadReturn[0] = new Checker();
                    ((Checker) threadReturn[0]).configure(config);

                } catch (CheckstyleException e) {
                    threadReturn[0] = e;
                }
            }
        };

        // Fetch the class loader from the JetStyle plugin
        final ClassLoader loader = getClass().getClassLoader();

        // Set the context Class loader
        worker.setContextClassLoader(loader);

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
            throw(CheckstyleException) threadReturn[0];
        }

        checker = (Checker) threadReturn[0];

        return checker;
    }

}
