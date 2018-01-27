package org.infernus.idea.checkstyle.config;

import net.jcip.annotations.Immutable;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;


/**
 * Represents the entire persistent plugin configuration on project level as an immutable object.
 * This is intended to be a simple DTO without any business logic.
 */
@Immutable
public class PluginConfigDto {
    private final String checkstyleVersion;
    private final ScanScope scanScope;
    private final boolean suppressErrors;
    private final boolean copyLibs;
    private final SortedSet<ConfigurationLocation> locations;
    private final List<String> thirdPartyClasspath;
    private final ConfigurationLocation activeLocation;
    private final boolean scanBeforeCheckin;
    private final String lastActivePluginVersion;

    PluginConfigDto(@NotNull final String checkstyleVersion,
                    @NotNull final ScanScope scanScope,
                    final boolean suppressErrors,
                    final boolean copyLibs,
                    @NotNull final SortedSet<ConfigurationLocation> locations,
                    @NotNull final List<String> thirdPartyClasspath,
                    @Nullable final ConfigurationLocation activeLocation,
                    final boolean scanBeforeCheckin,
                    @Nullable final String lastActivePluginVersion) {
        this.checkstyleVersion = checkstyleVersion;
        this.scanScope = scanScope;
        this.suppressErrors = suppressErrors;
        this.copyLibs = copyLibs;
        this.locations = Collections.unmodifiableSortedSet(locations);
        this.thirdPartyClasspath = Collections.unmodifiableList(thirdPartyClasspath);
        this.activeLocation = constrainActiveLocation(locations, activeLocation);
        this.scanBeforeCheckin = scanBeforeCheckin;
        this.lastActivePluginVersion = lastActivePluginVersion;
    }

    private ConfigurationLocation constrainActiveLocation(
            @NotNull final SortedSet<ConfigurationLocation> sourceLocations,
            @Nullable final ConfigurationLocation sourceActiveLocation) {
        if (sourceActiveLocation != null && !sourceLocations.isEmpty()) {
            return sourceLocations.stream()
                    .filter(cl -> cl.equals(sourceActiveLocation))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    @NotNull
    public String getCheckstyleVersion() {
        return checkstyleVersion;
    }

    @NotNull
    public ScanScope getScanScope() {
        return scanScope;
    }

    public boolean isSuppressErrors() {
        return suppressErrors;
    }

    public boolean isCopyLibs() {
        return copyLibs;
    }

    @NotNull
    public SortedSet<ConfigurationLocation> getLocations() {
        return locations;
    }

    @NotNull
    public List<String> getThirdPartyClasspath() {
        return thirdPartyClasspath;
    }

    @Nullable
    public String getLastActivePluginVersion() {
        return lastActivePluginVersion;
    }

    @Nullable
    public ConfigurationLocation getActiveLocation() {
        return activeLocation;
    }

    public boolean isScanBeforeCheckin() {
        return scanBeforeCheckin;
    }

    public boolean hasChangedFrom(final Object other) {
        return this.equals(other) && locationsAreEqual((PluginConfigDto) other);
    }

    private boolean locationsAreEqual(final PluginConfigDto other) {
        Iterator<ConfigurationLocation> locationIterator = locations.iterator();
        Iterator<ConfigurationLocation> otherLocationIterator = other.locations.iterator();

        while (locationIterator.hasNext() && otherLocationIterator.hasNext()) {
            try {
                if (locationIterator.next().hasChangedFrom(otherLocationIterator.next())) {
                    return false;
                }
            } catch (IOException e) {
                throw new CheckStylePluginException("Unable to test configuration properties for changes", e);
            }
        }

        return locations.size() == other.locations.size();
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final PluginConfigDto otherDto = (PluginConfigDto) other;
        return Objects.equals(checkstyleVersion, otherDto.checkstyleVersion)
                && Objects.equals(scanScope, otherDto.scanScope)
                && Objects.equals(suppressErrors, otherDto.suppressErrors)
                && Objects.equals(copyLibs, otherDto.copyLibs)
                && Objects.equals(locations, otherDto.locations)
                && Objects.equals(thirdPartyClasspath, otherDto.thirdPartyClasspath)
                && Objects.equals(activeLocation, otherDto.activeLocation)
                && Objects.equals(scanBeforeCheckin, otherDto.scanBeforeCheckin)
                && Objects.equals(lastActivePluginVersion, otherDto.lastActivePluginVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkstyleVersion, scanScope, suppressErrors, copyLibs, locations, thirdPartyClasspath,
                activeLocation, scanBeforeCheckin, lastActivePluginVersion);
    }
}
