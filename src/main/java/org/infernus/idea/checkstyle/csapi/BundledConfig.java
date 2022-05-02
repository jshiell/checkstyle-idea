package org.infernus.idea.checkstyle.csapi;

import org.jetbrains.annotations.NotNull;


/**
 * The configuration files bundled with Checkstyle-IDEA as provided by Checkstyle.
 */
public enum BundledConfig {

    /** the Sun checks */
    SUN_CHECKS("(bundled)", "Sun Checks", "/sun_checks.xml"),

    /** the Google checks */
    GOOGLE_CHECKS("(bundled)", "Google Checks", "/google_checks.xml");


    private final String location;

    private final String description;

    private final String path;


    BundledConfig(@NotNull final String location, @NotNull final String description,
                  @NotNull final String path) {
        this.location = location;
        this.description = description;
        this.path = path;
    }


    @NotNull
    public String getLocation() {
        return location;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    @NotNull
    public String getPath() {
        return path;
    }


    @NotNull
    public static BundledConfig fromDescription(@NotNull final String pDescription) {
        BundledConfig result = GOOGLE_CHECKS;
        if (pDescription.contains("Sun")) {
            result = SUN_CHECKS;
        }
        return result;
    }
}
