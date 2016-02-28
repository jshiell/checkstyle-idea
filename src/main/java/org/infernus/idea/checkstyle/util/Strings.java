package org.infernus.idea.checkstyle.util;

public final class Strings {

    private Strings() {
    }

    public static boolean isBlank(final String value) {
        return value == null || value.trim().length() == 0;
    }

}
