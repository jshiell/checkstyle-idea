package org.infernus.idea.checkstyle.util;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Expands <code>${name}</code> references in user-defined property values against the plugin's
 * built-in properties.
 */
public final class PropertyExpander {

    private PropertyExpander() {
    }

    @NotNull
    public static Map<String, String> expand(@NotNull final Map<String, String> userProperties,
                                             @NotNull final Map<String, String> builtIns) {
        final Map<String, String> expanded = new HashMap<>();
        for (final Map.Entry<String, String> userProperty : userProperties.entrySet()) {
            expanded.put(userProperty.getKey(), expandValue(userProperty.getValue(), builtIns));
        }
        return expanded;
    }

    private static String expandValue(final String value, final Map<String, String> builtIns) {
        return builtIns.getOrDefault(nameReferencedBy(value), value);
    }

    private static String nameReferencedBy(final String value) {
        return value.substring("${".length(), value.length() - "}".length());
    }
}
