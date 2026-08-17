package org.infernus.idea.checkstyle.util;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Expands <code>${name}</code> references in user-defined property values against the plugin's
 * built-in properties.
 */
public final class PropertyExpander {

    private static final String REFERENCE_PREFIX = "${";
    private static final String REFERENCE_SUFFIX = "}";

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
        final int referenceStart = value.indexOf(REFERENCE_PREFIX);
        if (referenceStart < 0) {
            return value;
        }
        final int referenceEnd = value.indexOf(REFERENCE_SUFFIX, referenceStart);
        if (referenceEnd < 0) {
            return value;
        }

        final String name = value.substring(referenceStart + REFERENCE_PREFIX.length(), referenceEnd);
        final String builtIn = builtIns.get(name);
        if (builtIn == null) {
            return value;
        }

        return value.substring(0, referenceStart)
                + builtIn
                + value.substring(referenceEnd + REFERENCE_SUFFIX.length());
    }
}
