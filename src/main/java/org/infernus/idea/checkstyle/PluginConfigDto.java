package org.infernus.idea.checkstyle;

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
    private final SortedSet<ConfigurationLocation> locations;
    private final List<String> thirdPartyClasspath;
    private final ConfigurationLocation activeLocation;
    private final boolean scanBeforeCheckin;

    public PluginConfigDto(@NotNull final String checkstyleVersion,
                           @Nullable final ScanScope scanScope,
                           final boolean suppressErrors,
                           @NotNull final SortedSet<ConfigurationLocation> locations,
                           @NotNull final List<String> thirdPartyClasspath,
                           @Nullable final ConfigurationLocation activeLocation,
                           final boolean scanBeforeCheckin) {
        this.checkstyleVersion = checkstyleVersion;
        this.scanScope = scanScope != null ? scanScope : ScanScope.getDefaultValue();
        this.suppressErrors = suppressErrors;
        this.locations = Collections.unmodifiableSortedSet(locations);
        this.thirdPartyClasspath = Collections.unmodifiableList(thirdPartyClasspath);
        this.activeLocation = constrainActiveLocation(locations, activeLocation);
        this.scanBeforeCheckin = scanBeforeCheckin;
    }

    public PluginConfigDto(@NotNull final PluginConfigDto oldDto, final boolean pScanBeforeCheckin) {
        this(oldDto.getCheckstyleVersion(), oldDto.getScanScope(), oldDto.isSuppressErrors(), oldDto.getLocations(),
                oldDto.getThirdPartyClasspath(), oldDto.getActiveLocation(), pScanBeforeCheckin);
    }

    private final ConfigurationLocation constrainActiveLocation(
            @NotNull final SortedSet<ConfigurationLocation> locations,
            @Nullable final ConfigurationLocation activeLocation) {
        if (activeLocation != null && !locations.isEmpty()) {
            return locations.stream().filter(cl -> cl.equals(activeLocation)).findFirst().orElse(null);
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

    @NotNull
    public SortedSet<ConfigurationLocation> getLocations() {
        return locations;
    }

    @NotNull
    public List<String> getThirdPartyClasspath() {
        return thirdPartyClasspath;
    }

    /**
     * Getter.
     *
     * @return the active location, or <code>null</code> if none is active
     */
    @Nullable
    public ConfigurationLocation getActiveLocation() {
        return activeLocation;
    }

    public boolean isScanBeforeCheckin() {
        return scanBeforeCheckin;
    }

    boolean hasChangedFrom(final Object other,
                           final boolean defaultProject) {
        return this.equals(other)
                && locationsAreEqual((PluginConfigDto) other, defaultProject);
    }

    private boolean locationsAreEqual(final PluginConfigDto other,
                                      final boolean defaultProject) {
        Iterator<ConfigurationLocation> locationIterator = locations.iterator();
        Iterator<ConfigurationLocation> otherLocationIterator = other.locations.iterator();

        while (locationIterator.hasNext() && otherLocationIterator.hasNext()) {
            try {
                if (locationIterator.next().hasChangedFrom(otherLocationIterator.next(), defaultProject)) {
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
                && Objects.equals(locations, otherDto.locations)
                && Objects.equals(thirdPartyClasspath, otherDto.thirdPartyClasspath)
                && Objects.equals(activeLocation, otherDto.activeLocation)
                && Objects.equals(scanBeforeCheckin, otherDto.scanBeforeCheckin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkstyleVersion, scanScope, suppressErrors, locations, thirdPartyClasspath,
                activeLocation, scanBeforeCheckin);
    }
}
