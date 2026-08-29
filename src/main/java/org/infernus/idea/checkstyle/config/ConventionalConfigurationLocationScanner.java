package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;
import org.infernus.idea.checkstyle.util.ProjectPaths;
import org.jetbrains.annotations.NotNull;

/**
 * Detects a Checkstyle configuration file at one of a fixed set of conventional, project-relative
 * locations, and merges the result into the plugin's persisted configuration.
 *
 * <p>Mirrors {@link org.infernus.idea.checkstyle.maven.MavenCheckstyleConfigurator}'s all-static
 * style and its reserved-id "add if present, remove if absent" merge pattern.</p>
 */
public final class ConventionalConfigurationLocationScanner {

    // Reserved ID — must not be used as a user-defined location ID.
    // On every rescan any location with this ID is replaced unconditionally.
    //
    // Note: ConfigurationLocationFactory's instance cache is keyed by equals() (description +
    // location + type — never id). If a location with the exact same description/path/type already
    // exists under a different id, create(..., RESERVED_ID, ...) will silently return that cached
    // object carrying its original id, not RESERVED_ID. Pre-existing property of the shared factory,
    // also present in MavenCheckstyleConfigurator; not fixed here.
    static final String RESERVED_ID = "conventional-config-location";
    static final String RESERVED_DESCRIPTION = "Detected Checkstyle Configuration";

    private static final List<String> CANDIDATE_LOCATIONS = List.of(
            "config/checkstyle/checkstyle.xml",
            "checkstyle.xml",
            "etc/checkstyle.xml");

    private ConventionalConfigurationLocationScanner() {
    }

    /**
     * The outcome of a {@link #rescan(Project)}, describing what changed (if anything), so that
     * callers can present an appropriate notification without re-deriving this from
     * {@link PluginConfiguration} state themselves.
     */
    public enum ScanOutcome {
        ADDED, REPLACED, REMOVED, UNCHANGED_PRESENT, UNCHANGED_ABSENT, NO_PROJECT_DIRECTORY
    }

    /**
     * Rescans the project for a conventional configuration file and merges the result into the
     * plugin's persisted configuration: added if found and not previously present, replaced if a
     * higher-priority candidate has appeared, removed if no longer present. Never touches any
     * location that does not carry {@link #RESERVED_ID}.
     */
    @NotNull
    public static ScanOutcome rescan(@NotNull final Project project) {
        final VirtualFile baseDir = project.getService(ProjectPaths.class).projectPath(project);
        if (baseDir == null) {
            return ScanOutcome.NO_PROJECT_DIRECTORY;
        }

        // Forces a synchronous, recursive refresh so the scan reflects current disk state even if
        // nothing changed it through the IDE (e.g. a file that appeared via `git pull`).
        VfsUtil.markDirtyAndRefresh(false, true, true, baseDir);

        final PluginConfigurationManager configManager = project.getService(PluginConfigurationManager.class);
        final PluginConfiguration current = configManager.getCurrent();
        final Optional<ConfigurationLocation> previousLocation = current.getLocationById(RESERVED_ID);

        final var locations = new TreeSet<>(current.getLocations());
        final var activeLocationIds = new TreeSet<>(current.getActiveLocationIds());
        locations.removeIf(location -> RESERVED_ID.equals(location.getId()));
        activeLocationIds.removeIf(RESERVED_ID::equals);

        final Optional<ConfigurationLocation> foundLocation = findConventionalConfigurationLocation(project, baseDir);
        foundLocation.ifPresent(location -> {
            locations.add(location);
            activeLocationIds.add(location.getId());
        });

        final PluginConfiguration updated = PluginConfigurationBuilder.from(current)
                .withLocations(locations)
                .withActiveLocationIds(activeLocationIds)
                .build();
        if (current.hasChangedFrom(updated)) {
            configManager.setCurrent(updated, true);
        }

        return outcomeFor(previousLocation, foundLocation);
    }

    @NotNull
    private static ScanOutcome outcomeFor(@NotNull final Optional<ConfigurationLocation> previousLocation,
                                          @NotNull final Optional<ConfigurationLocation> foundLocation) {
        if (previousLocation.isEmpty() && foundLocation.isEmpty()) {
            return ScanOutcome.UNCHANGED_ABSENT;
        }
        if (previousLocation.isEmpty()) {
            return ScanOutcome.ADDED;
        }
        if (foundLocation.isEmpty()) {
            return ScanOutcome.REMOVED;
        }
        return previousLocation.get().equals(foundLocation.get()) ? ScanOutcome.UNCHANGED_PRESENT : ScanOutcome.REPLACED;
    }

    @NotNull
    static Optional<ConfigurationLocation> findConventionalConfigurationLocation(
            @NotNull final Project project,
            @NotNull final VirtualFile baseDir) {
        for (final String candidate : CANDIDATE_LOCATIONS) {
            final VirtualFile found = baseDir.findFileByRelativePath(candidate);
            if (found != null && !found.isDirectory()) {
                return Optional.of(project.getService(ConfigurationLocationFactory.class)
                        .create(project, RESERVED_ID, ConfigurationType.PROJECT_RELATIVE, found.getPath(),
                                RESERVED_DESCRIPTION, NamedScopeHelper.getDefaultScope(project)));
            }
        }
        return Optional.empty();
    }
}
