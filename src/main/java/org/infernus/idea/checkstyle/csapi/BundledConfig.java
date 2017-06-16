package org.infernus.idea.checkstyle.csapi;

import org.jetbrains.annotations.NotNull;


/**
 * The configuration files bundled with Checkstyle-IDEA as provided by Checkstyle.
 */
public enum BundledConfig
{
    /** the Sun checks */
    SUN_CHECKS("/sun_checks.xml"),

    /** the Google checks */
    GOOGLE_CHECKS("/google_checks.xml");


    private final String path;


    private BundledConfig(@NotNull final String path) {
        this.path = path;
    }


    public String getPath() {
        return path;
    }
}
