package org.infernus.idea.checkstyle.csapi;

import org.jetbrains.annotations.NotNull;


/**
 * The configuration files bundled with Checkstyle-IDEA as provided by Checkstyle.
 */
public enum BundledConfig
{
    /** the Sun checks */
    SUN_CHECKS("/sun_checks.xml", "Sun Checks"),

    /** the Google checks */
    GOOGLE_CHECKS("/google_checks.xml", "Google Checks");


    private final String path;

    private final String description;


    private BundledConfig(@NotNull final String path, @NotNull final String description) {
        this.path = path;
        this.description = description;
    }


    public String getPath() {
        return path;
    }

    public String getDescription() {
        return description;
    }
}
