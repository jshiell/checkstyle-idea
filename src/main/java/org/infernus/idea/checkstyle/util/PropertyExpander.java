package org.infernus.idea.checkstyle.util;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static org.infernus.idea.checkstyle.util.Strings.isBlank;

/**
 * Expands <code>${name}</code> references in user-defined property values against the plugin's
 * built-in properties.
 * <p>
 * Expansion is a single pass against the built-ins only: one user property is never expanded into
 * another, and substituted text is never rescanned. This relies on ConfigurationLocation registering
 * only the <em>first</em> reference in each rules-file value, and pruning any property the rules file
 * doesn't mention - so an intermediate user property can never survive to be referenced. Should that
 * extractor ever register every reference, chaining becomes reachable and this class will need
 * resolution ordering and cycle detection.
 * <p>
 * A reference whose built-in is unknown, null or blank is left verbatim.
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
        final StringBuilder expanded = new StringBuilder();

        int cursor = 0;
        while (true) {
            final int referenceStart = value.indexOf(REFERENCE_PREFIX, cursor);
            if (referenceStart < 0) {
                break;
            }
            final int referenceEnd = value.indexOf(REFERENCE_SUFFIX, referenceStart);
            if (referenceEnd < 0) {
                break;
            }

            final String reference = value.substring(referenceStart, referenceEnd + REFERENCE_SUFFIX.length());
            final String builtIn = builtIns.get(
                    value.substring(referenceStart + REFERENCE_PREFIX.length(), referenceEnd));

            expanded.append(value, cursor, referenceStart)
                    .append(isBlank(builtIn) ? reference : builtIn);
            // resume after the replacement, so substituted text is never itself rescanned
            cursor = referenceEnd + REFERENCE_SUFFIX.length();
        }

        return expanded.append(value.substring(cursor)).toString();
    }
}
