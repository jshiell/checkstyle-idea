package org.infernus.idea.checkstyle.util;

public final class DisplayFormats {

    private DisplayFormats() {
    }

    public static String shortenClassName(final String className) {
        final int lastPackageIndex = className.lastIndexOf(".");
        if (lastPackageIndex >= 0) {
            return className
                    .substring(lastPackageIndex + 1)
                    .replaceFirst("Check$", "");
        }
        return className;
    }

}
