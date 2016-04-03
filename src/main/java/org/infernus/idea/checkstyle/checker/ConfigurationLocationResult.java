package org.infernus.idea.checkstyle.checker;

import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConfigurationLocationResult {

    public final ConfigurationLocation location;
    public final ConfigurationLocationStatus status;

    private ConfigurationLocationResult(final ConfigurationLocation location,
                                        final ConfigurationLocationStatus status) {
        this.location = location;
        this.status = status;
    }

    public static ConfigurationLocationResult resultOf(@Nullable final ConfigurationLocation configurationLocation,
                                                       @NotNull final ConfigurationLocationStatus status) {
        return new ConfigurationLocationResult(configurationLocation, status);
    }

    public static ConfigurationLocationResult resultOf(@NotNull final ConfigurationLocationStatus status) {
        return new ConfigurationLocationResult(null, status);
    }

}
