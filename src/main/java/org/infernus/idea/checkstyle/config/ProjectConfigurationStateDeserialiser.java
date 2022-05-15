package org.infernus.idea.checkstyle.config;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public abstract class ProjectConfigurationStateDeserialiser {

    public abstract PluginConfigurationBuilder deserialise(
            @NotNull PluginConfigurationBuilder builder,
            @NotNull Map<String, Object> projectConfiguration);


    protected boolean booleanValueOf(@NotNull final Map<String, Object> properties, final String propertyName) {
        return booleanValueOfWithDefault(properties, propertyName, false);
    }

    protected boolean booleanValueOfWithDefault(@NotNull final Map<String, Object> properties,
                                                final String propertyName,
                                                final boolean defaultValue) {
        final Object value = properties.get(propertyName);
        if (value != null) {
            return Boolean.parseBoolean(value.toString());
        }
        return defaultValue;
    }

}
