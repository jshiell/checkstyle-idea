package org.infernus.idea.checkstyle.config;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public abstract class ProjectConfigurationStateDeserialiser {

    public abstract PluginConfigurationBuilder deserialise(
            @NotNull PluginConfigurationBuilder builder,
            @NotNull Map<String, String> projectConfiguration);


    protected boolean booleanValueOf(@NotNull final Map<String, String> loadedMap, final String propertyName) {
        return Boolean.parseBoolean(loadedMap.get(propertyName));
    }

    protected boolean booleanValueOfWithDefault(@NotNull final Map<String, String> configuration,
                                                final String propertyName,
                                                final boolean defaultValue) {
        final String v = configuration.get(propertyName);
        return v != null ? Boolean.parseBoolean(v) : defaultValue;
    }

}
