package org.infernus.idea.checkstyle;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;

import net.jcip.annotations.Immutable;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Represents the entire persistent plugin configuration on project level as an immutable object.
 * This is intended to be a simple DTO without any business logic.
 */
@Immutable
public class PluginConfigDto
{
    private final String checkstyleVersion;

    private final ScanScope scanScope;

    private final boolean suppressErrors;

    private final SortedSet<ConfigurationLocation> locations;

    private final List<String> thirdPartyClasspath;

    private final ConfigurationLocation activeLocation;

    private final boolean scanBeforeCheckin;


    public PluginConfigDto(@NotNull final String checkstyleVersion, @Nullable final ScanScope scanScope,
                           final boolean suppressErrors, @NotNull final SortedSet<ConfigurationLocation> locations,
                           @NotNull final List<String> thirdPartyClasspath,
                           @Nullable final ConfigurationLocation activeLocation, final boolean scanBeforeCheckin) {
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


    @Override
    public boolean equals(final Object pOther) {
        if (this == pOther) {
            return true;
        }
        if (pOther == null || getClass() != pOther.getClass()) {
            return false;
        }
        final PluginConfigDto other = (PluginConfigDto) pOther;
        return Objects.equals(checkstyleVersion, other.checkstyleVersion)
                && Objects.equals(scanScope, other.scanScope)
                && Objects.equals(suppressErrors, other.suppressErrors)
                && Objects.equals(locations, other.locations)
                && Objects.equals(thirdPartyClasspath, other.thirdPartyClasspath)
                && Objects.equals(activeLocation, other.activeLocation)
                && Objects.equals(scanBeforeCheckin, other.scanBeforeCheckin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkstyleVersion, scanScope, suppressErrors, locations, thirdPartyClasspath,
                activeLocation, scanBeforeCheckin);
    }
}
