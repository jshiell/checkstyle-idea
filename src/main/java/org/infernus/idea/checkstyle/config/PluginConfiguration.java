package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import net.jcip.annotations.Immutable;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
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
    private final SortedSet<String> activeLocationDescriptors;
    private final boolean scanBeforeCheckin;
    private final String lastActivePluginVersion;

    PluginConfiguration(@NotNull final String checkstyleVersion,
                        @NotNull final ScanScope scanScope,
                        final boolean suppressErrors,
                        final boolean copyLibs,
                        @NotNull final SortedSet<ConfigurationLocation> locations,
                        @NotNull final List<String> thirdPartyClasspath,
                        @NotNull final SortedSet<String> activeLocationDescriptors,
                        final boolean scanBeforeCheckin,
                        @Nullable final String lastActivePluginVersion) {
        this.checkstyleVersion = checkstyleVersion;
        this.scanScope = scanScope;
        this.suppressErrors = suppressErrors;
        this.copyLibs = copyLibs;
        this.locations = Collections.unmodifiableSortedSet(locations);
        this.thirdPartyClasspath = Collections.unmodifiableList(thirdPartyClasspath);
        this.activeLocationDescriptors = activeLocationDescriptors.stream()
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

    private static final Logger LOG = Logger.getInstance(PluginConfiguration.class);

    @NotNull
    public Optional<ConfigurationLocation> findByDescriptor(@NotNull final String locationDescriptorToFind,
                                                            @NotNull final Project project) {
        // This is a horrid hack to get around IDEA resolving $PROJECT_DIR$ in descriptors, hence we need to do the whole load
        // logic to make things consistent
        // Ideally we should switch from descriptors to GUIDs or similar to avoid such things, although we'd
        // still need the logic for legacy values
        ConfigurationLocation resolvedConfiguration = configurationLocationFactory(project).create(project, locationDescriptorToFind);

        return locations.stream()
                .filter(location -> location.getDescriptor().equals(resolvedConfiguration.getDescriptor()))
                .limit(1)
                .findFirst();
    }

    private ConfigurationLocationFactory configurationLocationFactory(final Project project) {
        return ServiceManager.getService(project, ConfigurationLocationFactory.class);
    }

    @NotNull
    public List<String> getThirdPartyClasspath() {
        return thirdPartyClasspath;
    }

    @Nullable
    public String getLastActivePluginVersion() {
        return lastActivePluginVersion;
    }

    public SortedSet<String> getActiveLocationDescriptors() {
        return this.activeLocationDescriptors;
    }

    @NotNull
    public SortedSet<ConfigurationLocation> getActiveLocations(@NotNull final Project project) {
        return getActiveLocationDescriptors().stream()
                .map(activeLocationDescriptor -> findByDescriptor(activeLocationDescriptor, project))
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
                && Objects.equals(activeLocationDescriptors, otherDto.activeLocationDescriptors)
                && Objects.equals(scanBeforeCheckin, otherDto.scanBeforeCheckin)
                && Objects.equals(lastActivePluginVersion, otherDto.lastActivePluginVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkstyleVersion, scanScope, suppressErrors, copyLibs, locations, thirdPartyClasspath,
                activeLocationDescriptors, scanBeforeCheckin, lastActivePluginVersion);
    }

}
