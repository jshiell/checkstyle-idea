package org.infernus.idea.checkstyle.util;

import java.util.Locale;

public final class OS {

    private static final String OPERATING_SYSTEM = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

    private OS() {
    }

    public static boolean isWindows() {
        return OPERATING_SYSTEM.contains("win");
    }
}
