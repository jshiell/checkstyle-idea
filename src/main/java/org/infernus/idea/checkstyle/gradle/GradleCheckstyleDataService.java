package org.infernus.idea.checkstyle.gradle;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import org.infernus.idea.checkstyle.VersionListReader;
import org.infernus.idea.checkstyle.config.PluginConfiguration;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Applies {@link CheckstyleGradleModuleData} gathered during Gradle sync to {@link PluginConfiguration},
 * the literal analogue of {@code MavenCheckstyleConfigurator.afterImport} for Gradle. Mirrors that
 * class's opt-in flag, reserved-location-id warn-and-replace, and unconditional-remove-then-conditional
 * -add behaviour so the two integrations are indistinguishable to a user picking between them.
 */
public class GradleCheckstyleDataService implements ProjectDataService<CheckstyleGradleModuleData, Void> {

    private static final Logger LOG = Logger.getInstance(GradleCheckstyleDataService.class);

    // Reserved ID — must not be used as a user-defined location ID.
    // On every Gradle sync any location with this ID is replaced unconditionally.
    static final String GRADLE_CONFIG_LOCATION_ID = "gradle-config-location";
    static final String GRADLE_CONFIG_LOCATION_DESCRIPTION = "Gradle Config Location";

    @NotNull
    @Override
    public Key<CheckstyleGradleModuleData> getTargetDataKey() {
        return CheckstyleGradleModuleData.KEY;
    }

    @Override
    public void importData(@NotNull final Collection<? extends DataNode<CheckstyleGradleModuleData>> toImport,
                            @Nullable final ProjectData projectData,
                            @NotNull final Project project,
                            @NotNull final IdeModifiableModelsProvider modelsProvider) {
        // Intentionally empty: all work happens in onSuccessImport, which needs no
        // IdeModifiableModelsProvider write-lock since it only touches this plugin's own
        // PluginConfigurationManager.
    }

    @NotNull
    @Override
    public Computable<Collection<Void>> computeOrphanData(
            @NotNull final Collection<? extends DataNode<CheckstyleGradleModuleData>> toImport,
            @NotNull final ProjectData projectData,
            @NotNull final Project project,
            @NotNull final IdeModifiableModelsProvider modelsProvider) {
        return List::of;
    }

    @Override
    public void removeData(@NotNull final Computable<? extends Collection<? extends Void>> toRemove,
                            @NotNull final Collection<? extends DataNode<CheckstyleGradleModuleData>> toIgnore,
                            @NotNull final ProjectData projectData,
                            @NotNull final Project project,
                            @NotNull final IdeModifiableModelsProvider modelsProvider) {
        // No-op: orphan removal is never produced by computeOrphanData above.
    }

    @Override
    public void onSuccessImport(@NotNull final Collection<DataNode<CheckstyleGradleModuleData>> imported,
                                 @Nullable final ProjectData projectData,
                                 @NotNull final Project project,
                                 @NotNull final IdeModelsProvider modelsProvider) {
        final var pluginConfigurationManager = project.getService(PluginConfigurationManager.class);
        final var currentPluginConfiguration = pluginConfigurationManager.getCurrent();

        // Require users to opt in to avoid a breaking change.
        if (!currentPluginConfiguration.isImportSettingsFromGradle()) {
            LOG.debug("Gradle settings import is disabled");
            return;
        }

        final var pluginConfigurationBuilder = PluginConfigurationBuilder.from(currentPluginConfiguration);

        final var configLocations = new TreeSet<>(currentPluginConfiguration.getLocations());
        pluginConfigurationBuilder.withLocations(configLocations);

        final var activeConfigLocationIds = new TreeSet<>(currentPluginConfiguration.getActiveLocationIds());
        pluginConfigurationBuilder.withActiveLocationIds(activeConfigLocationIds);

        currentPluginConfiguration.getLocations().stream()
                .filter(loc -> GRADLE_CONFIG_LOCATION_ID.equals(loc.getId()))
                .filter(loc -> !GRADLE_CONFIG_LOCATION_DESCRIPTION.equals(loc.getDescription()))
                .forEach(loc -> LOG.warn("Replacing non-Gradle-managed location carrying reserved id '"
                        + GRADLE_CONFIG_LOCATION_ID + "': " + loc));

        configLocations.removeIf(location -> GRADLE_CONFIG_LOCATION_ID.equals(location.getId()));
        activeConfigLocationIds.removeIf(GRADLE_CONFIG_LOCATION_ID::equals);

        final Optional<CheckstyleGradleModuleData> selected = imported.stream()
                .map(DataNode::getData)
                .filter(data -> data.getConfigFile() != null)
                .min(Comparator.comparing(CheckstyleGradleModuleData::getGradleProjectPath));

        if (selected.isPresent()) {
            final CheckstyleGradleModuleData data = selected.get();
            final ConfigurationLocation location = createConfigurationLocation(project, data);
            configLocations.add(location);
            activeConfigLocationIds.add(location.getId());

            applyToolVersion(data.getToolVersion(), currentPluginConfiguration, pluginConfigurationBuilder);
        }
        // If no incoming module has a usable configFile, the reserved-id location has already been
        // removed above and nothing further needs to happen here.

        final var newPluginConfiguration = pluginConfigurationBuilder.build();
        if (currentPluginConfiguration.hasChangedFrom(newPluginConfiguration)) {
            pluginConfigurationManager.setCurrent(newPluginConfiguration, true);
        }
    }

    @NotNull
    private static ConfigurationLocation createConfigurationLocation(@NotNull final Project project,
                                                                       @NotNull final CheckstyleGradleModuleData data) {
        final File resolvedFile = new File(Objects.requireNonNull(data.getConfigFile()));
        final ConfigurationType type = isUnderProjectBaseDir(project, resolvedFile)
                ? ConfigurationType.PROJECT_RELATIVE : ConfigurationType.LOCAL_FILE;

        final var configurationLocationFactory = project.getService(ConfigurationLocationFactory.class);
        final ConfigurationLocation location = configurationLocationFactory.create(project,
                GRADLE_CONFIG_LOCATION_ID, type, resolvedFile.getAbsolutePath(), GRADLE_CONFIG_LOCATION_DESCRIPTION,
                NamedScopeHelper.getDefaultScope(project));
        location.setProperties(data.getConfigProperties());
        return location;
    }

    private static boolean isUnderProjectBaseDir(@NotNull final Project project, @NotNull final File file) {
        final String basePath = project.getBasePath();
        if (basePath == null) {
            return false;
        }
        return Path.of(file.getAbsolutePath()).startsWith(Path.of(basePath));
    }

    private static void applyToolVersion(@Nullable final String toolVersion,
                                          @NotNull final PluginConfiguration currentPluginConfiguration,
                                          @NotNull final PluginConfigurationBuilder pluginConfigurationBuilder) {
        if (toolVersion == null) {
            return;
        }

        if (new VersionListReader().getSupportedVersions().contains(toolVersion)) {
            pluginConfigurationBuilder.withCheckstyleVersion(toolVersion);
        } else {
            LOG.warn("Gradle project reports Checkstyle tool version '" + toolVersion
                    + "', which is not a version this plugin supports; leaving the current version ('"
                    + currentPluginConfiguration.getCheckstyleVersion() + "') unchanged");
        }
    }
}
