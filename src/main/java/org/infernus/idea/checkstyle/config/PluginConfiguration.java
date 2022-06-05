package org.infernus.idea.checkstyle.config;

import net.jcip.annotations.Immutable;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Represents the entire persistent plugin configuration on project level as an immutable object.
 * This is intended to be a simple DTO without any business logic.
 */
@Immutable
public class PluginConfiguration {
    private final String checkstyleVersion;
    private final ScanScope scanScope;
    private final boolean suppressErrors;
    private final boolean copyLibs;
    private final SortedSet<ConfigurationLocation> locations;
    private final List<String> thirdPartyClasspath;
    private final SortedSet<String> activeLocationIds;
    private final boolean scanBeforeCheckin;
    private final String lastActivePluginVersion;

    PluginConfiguration(@NotNull final String checkstyleVersion,
                        @NotNull final ScanScope scanScope,
                        final boolean suppressErrors,
                        final boolean copyLibs,
                        @NotNull final SortedSet<ConfigurationLocation> locations,
                        @NotNull final List<String> thirdPartyClasspath,
                        @NotNull final SortedSet<String> activeLocationIds,
                        final boolean scanBeforeCheckin,
                        @Nullable final String lastActivePluginVersion) {
        this.checkstyleVersion = checkstyleVersion;
        this.scanScope = scanScope;
        this.suppressErrors = suppressErrors;
        this.copyLibs = copyLibs;
        this.locations = Collections.unmodifiableSortedSet(locations);
        this.thirdPartyClasspath = Collections.unmodifiableList(thirdPartyClasspath);
        this.activeLocationIds = activeLocationIds.stream()
                .filter(Objects::nonNull)
		        .collect(Collectors.toCollection(TreeSet::new));
        this.scanBeforeCheckin = scanBeforeCheckin;
        this.lastActivePluginVersion = lastActivePluginVersion;
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
    public Optional<ConfigurationLocation> getLocationById(@NotNull final String locationId) {
        return locations.stream()
                .filter(candidate -> candidate.getId().equals(locationId))
                .findFirst();
    }

    @NotNull
    public List<String> getThirdPartyClasspath() {
        return thirdPartyClasspath;
    }

    @Nullable
    public String getLastActivePluginVersion() {
        return lastActivePluginVersion;
    }

    public SortedSet<String> getActiveLocationIds() {
        return this.activeLocationIds;
    }

    @NotNull
    public SortedSet<ConfigurationLocation> getActiveLocations() {
        return getActiveLocationIds().stream()
                .map(idToFind -> locations.stream().filter(candidate -> candidate.getId().equals(idToFind)).findFirst())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public boolean isScanBeforeCheckin() {
        return scanBeforeCheckin;
    }

    public boolean hasChangedFrom(final Object other) {
        return this.equals(other) && locationsAreEqual((PluginConfiguration) other);
    }

    private boolean locationsAreEqual(final PluginConfiguration other) {
        Iterator<ConfigurationLocation> locationIterator = locations.iterator();
        Iterator<ConfigurationLocation> otherLocationIterator = other.locations.iterator();

        while (locationIterator.hasNext() && otherLocationIterator.hasNext()) {
            if (locationIterator.next().hasChangedFrom(otherLocationIterator.next())) {
                return false;
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
        final PluginConfiguration otherDto = (PluginConfiguration) other;
        return Objects.equals(checkstyleVersion, otherDto.checkstyleVersion)
                && Objects.equals(scanScope, otherDto.scanScope)
                && Objects.equals(suppressErrors, otherDto.suppressErrors)
                && Objects.equals(copyLibs, otherDto.copyLibs)
                && Objects.equals(locations, otherDto.locations)
                && Objects.equals(thirdPartyClasspath, otherDto.thirdPartyClasspath)
                && Objects.equals(activeLocationIds, otherDto.activeLocationIds)
                && Objects.equals(scanBeforeCheckin, otherDto.scanBeforeCheckin)
                && Objects.equals(lastActivePluginVersion, otherDto.lastActivePluginVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkstyleVersion, scanScope, suppressErrors, copyLibs, locations, thirdPartyClasspath,
                activeLocationIds, scanBeforeCheckin, lastActivePluginVersion);
    }

}
