package org.infernus.idea.checkstyle.csapi;


import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class ConfigurationModule
{
    private final String name;

    private final Map<String, String> properties;

    private final Set<KnownTokenTypes> knownTokenTypes;

    private final Map<String, String> messages;


    public ConfigurationModule(@NotNull final String pName, @Nullable final Map<String, String> pProperties,
            @Nullable final Set<KnownTokenTypes> pKnownTokenTypes, @Nullable final Map<String, String> pMessages) {

        name = pName;

        if (pProperties != null) {
            properties = Collections.unmodifiableMap(pProperties);
        } else {
            properties = Collections.emptyMap();
        }

        if (pKnownTokenTypes != null) {
            knownTokenTypes = Collections.unmodifiableSet(pKnownTokenTypes);
        } else {
            knownTokenTypes = Collections.emptySet();
        }

        if (pMessages != null) {
            messages = Collections.unmodifiableMap(pMessages);
        } else {
            messages = Collections.emptyMap();
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
