package org.infernus.idea.checkstyle;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A manager for CheckStyle plug-in configuration.
 *
 * @author James Shiell
 * @version 1.0
 */
public final class CheckStyleConfiguration extends Properties {

    /**
     * The CP location of the default CheckStyle configuration.
     */
    public static final String DEFAULT_CONFIG = "/sun_checks.xml";

    /**
     * The CheckStyle file path.
     */
    public static final String CONFIG_FILE = "config-file";

    /**
     * Should test classes be checked?
     */
    public static final String CHECK_TEST_CLASSES = "check-test-classes";

    /**
     * The CheckStyle file path.
     */
    public static final String THIRDPARTY_CLASSPATH = "thirdparty-classpath";

    /**
     * The prefix for stored properties.
     */
    public static final String PROPERTIES_PREFIX = "property.";

    /**
     * Get all CheckStyle properties defined in the configuration.
     *
     * @return a map of CheckStyle property names to values.
     */
    public Map<String, String> getDefinedProperies() {
        final Map<String, String> values = new HashMap<String, String>();

        for (final Enumeration properties = propertyNames();
             properties.hasMoreElements();) {
            final String propertyName = (String) properties.nextElement();
            if (propertyName.startsWith(PROPERTIES_PREFIX)) {
                final String configPropertyName = propertyName.substring(
                        PROPERTIES_PREFIX.length());
                final String configPropertyValue = getProperty(
                        propertyName);

                values.put(configPropertyName, configPropertyValue);
            }
        }

        return values;
    }

    /**
     * Set the passed CheckStyle properties in the configuration.
     * <p/>
     * This will not erase old values. Use {@link #clearDefinedProperies()}
     * for that.
     *
     * @param properties a map of CheckStyle property names to values.
     */
    public void setDefinedProperies(final Map<String, String> properties) {
        if (properties == null || properties.size() == 0) {
            return;
        }

        for (final String propertyName : properties.keySet()) {
            final String value = properties.get(propertyName);
            if (value != null) {
                setProperty(PROPERTIES_PREFIX + propertyName, value);
            }
        }
    }

    /**
     * Clear all CheckStyle properties defined in the configuration.
     */
    public void clearDefinedProperies() {
        final List<String> propertiesToRemove = new ArrayList<String>();

        for (final Enumeration properties = propertyNames();
             properties.hasMoreElements();) {
            final String propertyName = (String) properties.nextElement();
            if (propertyName.startsWith(PROPERTIES_PREFIX)) {
                // delay to stop concurrent modification
                propertiesToRemove.add(propertyName);
            }
        }

        for (final String property : propertiesToRemove) {
            remove(property);
        }
    }

    /**
     * Get a string list property value.
     *
     * @param propertyName the name of the property.
     * @return the value of the property.
     */
    @NotNull
    public List<String> getListProperty(final String propertyName) {
        final List<String> returnValue = new ArrayList<String>();

        final String value = getProperty(propertyName);
        if (value != null) {
            final String[] parts = value.split(";");
            returnValue.addAll(Arrays.asList(parts));
        }

        return returnValue;
    }

    /**
     * Set a string list property value.
     *
     * @param propertyName the name of the property.
     * @param value the value of the property.
     */
    public void setProperty(final String propertyName,
                            final List<String> value) {
        if (value == null) {
            setProperty(propertyName, (String) null);
            return;
        }

        final StringBuilder valueString = new StringBuilder();
        for (final String part : value) {
            if (valueString.length() > 0) {
                valueString.append(";");
            }
            valueString.append(part);
        }

        setProperty(propertyName, valueString.toString());
    }

    /**
     * Get a boolean property value.
     *
     * @param propertyName the name of the property.
     * @param defaultValue the default value if the property is not set.
     * @return the value of the property.
     */
    public boolean getBooleanProperty(final String propertyName,
                                      final boolean defaultValue) {
        return Boolean.valueOf(getProperty(propertyName,
                Boolean.toString(defaultValue)));
    }

    /**
     * Set a boolean property value.
     *
     * @param propertyName the name of the property.
     * @param value the value of the property.
     */
    public void setProperty(final String propertyName,
                            final boolean value) {
        setProperty(propertyName, Boolean.toString(value));
    }
}
