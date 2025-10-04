package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.VersionListReader;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.infernus.idea.checkstyle.util.OS;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class PluginConfigurationBuilder {
    private String checkstyleVersion;
    private ScanScope scanScope;
    private boolean suppressErrors;
    private boolean copyLibraries;
    private boolean scrollToSource;
    private SortedSet<ConfigurationLocation> locations;
    private List<String> thirdPartyClasspath;
    private SortedSet<String> activeLocationIds;
    private boolean scanBeforeCheckin;

    private PluginConfigurationBuilder(@NotNull final String checkstyleVersion,
                                       @NotNull final ScanScope scanScope,
                                       final boolean suppressErrors,
                                       final boolean copyLibraries,
                                       final boolean scrollToSource,
                                       @NotNull final SortedSet<ConfigurationLocation> locations,
                                       @NotNull final List<String> thirdPartyClasspath,
                                       @NotNull final SortedSet<String> activeLocationIds,
                                       final boolean scanBeforeCheckin) {
        this.checkstyleVersion = checkstyleVersion;
        this.scanScope = scanScope;
        this.suppressErrors = suppressErrors;
        this.copyLibraries = copyLibraries;
        this.scrollToSource = scrollToSource;
        this.locations = locations;
        this.thirdPartyClasspath = thirdPartyClasspath;
        this.activeLocationIds = activeLocationIds;
        this.scanBeforeCheckin = scanBeforeCheckin;
    }

    public static PluginConfigurationBuilder defaultConfiguration(@NotNull final Project project) {
        final String csDefaultVersion = new VersionListReader().getDefaultVersion();

        final SortedSet<ConfigurationLocation> defaultLocations = new TreeSet<>();

        BundledConfig.getAllBundledConfigs().stream().map(bc -> configurationLocationFactory(project).create(bc, project)).forEach(defaultLocations::add);

        final boolean copyLibs = OS.isWindows();

        return new PluginConfigurationBuilder(
                csDefaultVersion,
                ScanScope.getDefaultValue(),
                false,
                copyLibs,
                false,
                defaultLocations,
                Collections.emptyList(),
                Collections.emptySortedSet(),
                false);
    }

    public static PluginConfigurationBuilder testInstance(@NotNull final String checkstyleVersion) {
        return new PluginConfigurationBuilder(
                checkstyleVersion,
                ScanScope.AllSources,
                false,
                false,
                false,
                Collections.emptySortedSet(),
                Collections.emptyList(),
                Collections.emptySortedSet(),
                false);
    }

    public static PluginConfigurationBuilder from(@NotNull final PluginConfiguration source) {
        return new PluginConfigurationBuilder(source.getCheckstyleVersion(),
                source.getScanScope(),
                source.isSuppressErrors(),
                source.isCopyLibs(),
                source.isScrollToSource(),
                source.getLocations(),
                source.getThirdPartyClasspath(),
                source.getActiveLocationIds(),
                source.isScanBeforeCheckin());
    }

    public PluginConfigurationBuilder withCheckstyleVersion(@NotNull final String newCheckstyleVersion) {
        this.checkstyleVersion = newCheckstyleVersion;
        return this;
    }

    public PluginConfigurationBuilder withActiveLocationIds(@NotNull final SortedSet<String> newActiveLocationIds) {
        this.activeLocationIds = newActiveLocationIds;
        return this;
    }

    public PluginConfigurationBuilder withSuppressErrors(final boolean newSuppressErrors) {
        this.suppressErrors = newSuppressErrors;
        return this;
    }

    public PluginConfigurationBuilder withCopyLibraries(final boolean newCopyLibraries) {
        this.copyLibraries = newCopyLibraries;
        return this;
    }

    public PluginConfigurationBuilder withScrollToSource(final boolean newScrollToSource) {
        this.scrollToSource = newScrollToSource;
        return this;
    }

    public PluginConfigurationBuilder withScanBeforeCheckin(final boolean newScanBeforeCheckin) {
        this.scanBeforeCheckin = newScanBeforeCheckin;
        return this;
    }

    public PluginConfigurationBuilder withLocations(@NotNull final SortedSet<ConfigurationLocation> newLocations) {
        this.locations = newLocations;
        return this;
    }

    public PluginConfigurationBuilder withThirdPartyClassPath(@NotNull final List<String> newThirdPartyClassPath) {
        this.thirdPartyClasspath = newThirdPartyClassPath;
        return this;
    }

    public PluginConfigurationBuilder withScanScope(@NotNull final ScanScope newScanScope) {
        this.scanScope = newScanScope;
        return this;
    }

    public PluginConfiguration build() {
        return new PluginConfiguration(
                checkstyleVersion,
                scanScope,
                suppressErrors,
                copyLibraries,
                scrollToSource,
                Objects.requireNonNullElseGet(locations, TreeSet::new),
                Objects.requireNonNullElseGet(thirdPartyClasspath, ArrayList::new),
                Objects.requireNonNullElseGet(activeLocationIds, TreeSet::new),
                scanBeforeCheckin);
    }

    private static ConfigurationLocationFactory configurationLocationFactory(final Project project) {
        return project.getService(ConfigurationLocationFactory.class);
    }
}
