package org.infernus.idea.checkstyle.checker;

import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ConfigurationLocationResult(ConfigurationLocation location, ConfigurationLocationStatus status) {

    public static final ConfigurationLocationResult NOT_PRESENT = new ConfigurationLocationResult(null, ConfigurationLocationStatus.NOT_PRESENT);

    public static ConfigurationLocationResult of(@Nullable final ConfigurationLocation configurationLocation,
                                                 @NotNull final ConfigurationLocationStatus status) {
        return new ConfigurationLocationResult(configurationLocation, status);
    }

}
