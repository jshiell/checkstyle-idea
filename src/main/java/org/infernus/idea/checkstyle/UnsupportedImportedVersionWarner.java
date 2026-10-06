package org.infernus.idea.checkstyle;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.util.Notifications;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Tells the user, once per project and version, that a build tool reported a Checkstyle version this plugin
 * neither supports nor replaces. Remembered per session only, so a restart warns again.
 */
public class UnsupportedImportedVersionWarner {

    @FunctionalInterface
    public interface Notifier {
        void showWarning(Project project, String text);
    }

    private final VersionListReader versionListReader = new VersionListReader();
    private final Notifier notifier;
    private final Map<Project, Set<String>> warnedVersionsByProject =
            Collections.synchronizedMap(new WeakHashMap<>());

    public UnsupportedImportedVersionWarner() {
        this(Notifications::showWarning);
    }

    public UnsupportedImportedVersionWarner(@NotNull final Notifier notifier) {
        this.notifier = notifier;
    }

    public void warnIfUnsupported(@NotNull final Project project,
                                  @NotNull final String buildTool,
                                  @NotNull final String reportedVersion,
                                  @NotNull final String keptVersion) {
        if (isUnresolvedPlaceholder(reportedVersion)
                || versionListReader.resolveSupportedVersion(reportedVersion).isPresent()
                || !markWarned(project, reportedVersion)) {
            return;
        }
        notifier.showWarning(project, CheckStyleBundle.message("notification.imported-version-unsupported",
                buildTool, reportedVersion, keptVersion));
    }

    private static boolean isUnresolvedPlaceholder(@NotNull final String version) {
        return version.contains("${");
    }

    private boolean markWarned(@NotNull final Project project, @NotNull final String version) {
        synchronized (warnedVersionsByProject) {
            return warnedVersionsByProject.computeIfAbsent(project, p -> new HashSet<>()).add(version);
        }
    }
}
