package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.VersionListReader;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.infernus.idea.checkstyle.util.OS;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public final class PluginConfigurationBuilder {
    private String checkstyleVersion;
    private ScanScope scanScope;
    private boolean suppressErrors;
    private boolean copyLibraries;
    private SortedSet<ConfigurationLocation> locations;
    private List<String> thirdPartyClasspath;
    private SortedSet<ConfigurationLocation> activeLocations;
    private boolean scanBeforeCheckin;
    private String lastActivePluginVersion;

    private PluginConfigurationBuilder(@NotNull final String checkstyleVersion,
                                       @NotNull final ScanScope scanScope,
                                       final boolean suppressErrors,
                                       final boolean copyLibraries,
                                       @NotNull final SortedSet<ConfigurationLocation> locations,
                                       @NotNull final List<String> thirdPartyClasspath,
                                       @NotNull final SortedSet<ConfigurationLocation> activeLocations,
                                       final boolean scanBeforeCheckin,
                                       @Nullable final String lastActivePluginVersion) {
        this.checkstyleVersion = checkstyleVersion;
        this.scanScope = scanScope;
        this.suppressErrors = suppressErrors;
        this.copyLibraries = copyLibraries;
        this.locations = locations;
        this.thirdPartyClasspath = thirdPartyClasspath;
        this.activeLocations = activeLocations;
        this.scanBeforeCheckin = scanBeforeCheckin;
        this.lastActivePluginVersion = lastActivePluginVersion;
    }

    public static PluginConfigurationBuilder defaultConfiguration(final Project project) {
        final String csDefaultVersion = new VersionListReader().getDefaultVersion();

        final SortedSet<ConfigurationLocation> defaultLocations = new TreeSet<>();
        defaultLocations.add(configurationLocationFactory(project).create(BundledConfig.SUN_CHECKS, project));
        defaultLocations.add(configurationLocationFactory(project).create(BundledConfig.GOOGLE_CHECKS, project));

        final boolean copyLibs = OS.isWindows();

        return new PluginConfigurationBuilder(
                csDefaultVersion,
                ScanScope.getDefaultValue(),
                false,
                copyLibs,
                defaultLocations,
                Collections.emptyList(),
                Collections.emptySortedSet(),
                false,
                CheckStylePlugin.version());
    }

    public static PluginConfigurationBuilder testInstance(final String checkstyleVersion) {
        return new PluginConfigurationBuilder(
                checkstyleVersion,
                ScanScope.AllSources,
                false,
                false,
                Collections.emptySortedSet(),
                Collections.emptyList(),
                Collections.emptySortedSet(),
                false,
                "aVersion");
    }

    public static PluginConfigurationBuilder from(final PluginConfiguration source) {
        return new PluginConfigurationBuilder(source.getCheckstyleVersion(),
                source.getScanScope(),
                source.isSuppressErrors(),
                source.isCopyLibs(),
                source.getLocations(),
                source.getThirdPartyClasspath(),
                source.getActiveLocations(),
                source.isScanBeforeCheckin(),
                source.getLastActivePluginVersion());
    }

    public PluginConfigurationBuilder withCheckstyleVersion(@Nullable final String newCheckstyleVersion) {
        this.checkstyleVersion = newCheckstyleVersion;
        return this;
    }

    public PluginConfigurationBuilder withActiveLocation(final SortedSet<ConfigurationLocation> newActiveLocation) {
        this.activeLocations = newActiveLocation;
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

    public PluginConfigurationBuilder withScanBeforeCheckin(final boolean newScanBeforeCheckin) {
        this.scanBeforeCheckin = newScanBeforeCheckin;
        return this;
    }

    public PluginConfigurationBuilder withLocations(final SortedSet<ConfigurationLocation> newLocations) {
        this.locations = newLocations;
        return this;
    }

    public PluginConfigurationBuilder withThirdPartyClassPath(final List<String> newThirdPartyClassPath) {
        this.thirdPartyClasspath = newThirdPartyClassPath;
        return this;
    }

    public PluginConfigurationBuilder withScanScope(@NotNull final ScanScope newScanScope) {
        this.scanScope = newScanScope;
        return this;
    }

    public PluginConfigurationBuilder withLastActivePluginVersion(final String newLastActivePluginVersion) {
        this.lastActivePluginVersion = newLastActivePluginVersion;
        return this;
    }

    public PluginConfiguration build() {
        return new PluginConfiguration(
                checkstyleVersion,
                scanScope,
                suppressErrors,
                copyLibraries,
                locations,
                thirdPartyClasspath,
                activeLocations,
                scanBeforeCheckin,
                lastActivePluginVersion);
    }

    private static ConfigurationLocationFactory configurationLocationFactory(final Project project) {
        return ServiceManager.getService(project, ConfigurationLocationFactory.class);
    }
}
