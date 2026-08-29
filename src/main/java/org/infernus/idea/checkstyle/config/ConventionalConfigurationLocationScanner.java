package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.List;
import java.util.Optional;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;
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
