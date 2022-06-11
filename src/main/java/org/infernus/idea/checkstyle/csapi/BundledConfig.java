package org.infernus.idea.checkstyle.csapi;

import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;


/**
 * The configuration files bundled with Checkstyle-IDEA as provided by Checkstyle.
 */
public enum BundledConfig {

    /** the Sun checks */
    SUN_CHECKS("bundled-sun-checks", "(bundled)", "Sun Checks", "/sun_checks.xml"),

    /** the Google checks */
    GOOGLE_CHECKS("bundled-google-checks", "(bundled)", "Google Checks", "/google_checks.xml");


    private final String id;

    private final String location;

    private final String description;

    private final String path;


    BundledConfig(@NotNull final String id,
                  @NotNull final String location,
                  @NotNull final String description,
                  @NotNull final String path) {
        this.id = id;
        this.location = location;
        this.description = description;
        this.path = path;
    }

    public String getId() {
        return id;
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

    public boolean matches(@NotNull final ConfigurationLocation configurationLocation) {
        return configurationLocation.getType() == ConfigurationType.BUNDLED
                && Objects.equals(configurationLocation.getLocation(), location)
                && Objects.equals(configurationLocation.getDescription(), description);
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
