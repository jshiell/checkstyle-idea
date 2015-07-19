package org.infernus.idea.checkstyle.checker;

import com.puppycrawl.tools.checkstyle.PropertyResolver;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ListPropertyResolver implements PropertyResolver {

    private final Map<String, String> propertyNamesToValues = new HashMap<>();

    private final List<String> propertyNames = new ArrayList<>();

    public ListPropertyResolver(final Map<String, String> properties) {
        setProperties(properties);
    }

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

        final String propertyValue = propertyNamesToValues.get(propertyName);
        if (propertyValue != null && propertyValue.trim().length() == 0) {
            return null;
        }
        return propertyValue;
    }

    public void setProperty(final String name, final String value) {
        if (!propertyNames.contains(name)) {
            propertyNames.add(name);
        }

        propertyNamesToValues.put(name, value);
    }

    public void setProperties(final Map<String, String> properties) {
        if (properties == null) {
            return;
        }

        for (final String propertyName : properties.keySet()) {
            setProperty(propertyName, properties.get(propertyName));
        }
    }
}
