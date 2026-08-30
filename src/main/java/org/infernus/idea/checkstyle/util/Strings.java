package org.infernus.idea.checkstyle.util;

public final class Strings {

    private Strings() {
    }

    public static boolean isBlank(final String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isHttpUrl(final String value) {
        if (isBlank(value)) {
            return false;
        }
        final String trimmed = value.trim().toLowerCase();
        return trimmed.startsWith("http://") || trimmed.startsWith("https://");
    }

}
