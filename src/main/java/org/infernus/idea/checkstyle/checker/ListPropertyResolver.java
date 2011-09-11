package org.infernus.idea.checkstyle.checker;

import com.puppycrawl.tools.checkstyle.PropertyResolver;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Property resolver using internal lists.
 */
class ListPropertyResolver implements PropertyResolver {

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
