package org.infernus.idea.checkstyle.csapi;


import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class ConfigurationModule {

    private final String name;

    private final Map<String, String> properties;

    private final Set<KnownTokenTypes> knownTokenTypes;

    private final Map<String, String> messages;


    public ConfigurationModule(@NotNull final String name,
                               @Nullable final Map<String, String> properties,
                               @Nullable final Set<KnownTokenTypes> knownTokenTypes,
                               @Nullable final Map<String, String> messages) {
        this.name = name;

        if (properties != null) {
            this.properties = Collections.unmodifiableMap(properties);
        } else {
            this.properties = Collections.emptyMap();
        }

        if (knownTokenTypes != null) {
            this.knownTokenTypes = Collections.unmodifiableSet(knownTokenTypes);
        } else {
            this.knownTokenTypes = Collections.emptySet();
        }

        if (messages != null) {
            this.messages = Collections.unmodifiableMap(messages);
        } else {
            this.messages = Collections.emptyMap();
        }
    }


    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public Map<String, String> getProperties() {
        return properties;
    }

    @NotNull
    public Set<KnownTokenTypes> getKnownTokenTypes() {
        return knownTokenTypes;
    }

    @NotNull
    public Map<String, String> getMessages() {
        return messages;
    }
}
