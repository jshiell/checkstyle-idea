package org.infernus.idea.checkstyle.util;

public class OS {

    private static final String OPERATING_SYSTEM = System.getProperty("os.name").toLowerCase();

    private OS() {
    }

    public static boolean isWindows() {
        return OPERATING_SYSTEM.contains("win");
    }
}
