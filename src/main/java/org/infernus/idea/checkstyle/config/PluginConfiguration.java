package org.infernus.idea.checkstyle.config;

import net.jcip.annotations.Immutable;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
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
    private final SortedSet<ConfigurationLocation> activeLocations;
    private final boolean scanBeforeCheckin;
    private final String lastActivePluginVersion;

    PluginConfiguration(@NotNull final String checkstyleVersion,
                        @NotNull final ScanScope scanScope,
                        final boolean suppressErrors,
                        final boolean copyLibs,
                        @NotNull final SortedSet<ConfigurationLocation> locations,
                        @NotNull final List<String> thirdPartyClasspath,
                        @NotNull final SortedSet<ConfigurationLocation> activeLocations,
                        final boolean scanBeforeCheckin,
                        @Nullable final String lastActivePluginVersion) {
        this.checkstyleVersion = checkstyleVersion;
        this.scanScope = scanScope;
        this.suppressErrors = suppressErrors;
        this.copyLibs = copyLibs;
        this.locations = Collections.unmodifiableSortedSet(locations);
        this.thirdPartyClasspath = Collections.unmodifiableList(thirdPartyClasspath);
        this.activeLocations = activeLocations.stream()
		        .map(e -> constrainActiveLocation(locations, e))
                .filter(Objects::nonNull)
		        .collect(Collectors.toCollection(TreeSet::new));
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

    public SortedSet<ConfigurationLocation> getActiveLocations() {
        return this.activeLocations;
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
                && Objects.equals(activeLocations, otherDto.activeLocations)
                && Objects.equals(scanBeforeCheckin, otherDto.scanBeforeCheckin)
                && Objects.equals(lastActivePluginVersion, otherDto.lastActivePluginVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkstyleVersion, scanScope, suppressErrors, copyLibs, locations, thirdPartyClasspath,
                activeLocations, scanBeforeCheckin, lastActivePluginVersion);
    }

}
