package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import java.util.Optional;
import org.infernus.idea.checkstyle.config.ConventionalConfigurationLocationScanner;
import org.infernus.idea.checkstyle.config.ConventionalConfigurationLocationScanner.ScanOutcome;
import org.infernus.idea.checkstyle.util.Notifications;
import org.jetbrains.annotations.NotNull;

import static org.infernus.idea.checkstyle.CheckStyleBundle.message;

/**
 * Manually rescans the project for a Checkstyle configuration file at one of a fixed set of
 * conventional locations, adding or removing it from the plugin configuration accordingly. The
 * "reload action" fallback proposed on issue #618, mirroring this plugin's Gradle/Maven sync
 * integrations.
 */
public class DetectConventionalConfigurationLocation extends BaseAction {

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        project(event).ifPresent(project -> {
            final ScanOutcome[] outcome = new ScanOutcome[1];
            ProgressManager.getInstance().run(new Task.Modal(project, message("detect.title"), false) {
                @Override
                public void run(@NotNull final ProgressIndicator indicator) {
                    outcome[0] = ConventionalConfigurationLocationScanner.rescan(project);
                }
            });
            messageFor(outcome[0]).ifPresent(msg -> Notifications.showInfo(project, msg));
        });
    }

    @NotNull
    static Optional<String> messageFor(@NotNull final ScanOutcome outcome) {
        return switch (outcome) {
            case ADDED -> Optional.of(message("notification.detect-conventional-config.added"));
            case REPLACED -> Optional.of(message("notification.detect-conventional-config.replaced"));
            case REMOVED -> Optional.of(message("notification.detect-conventional-config.removed"));
            case UNCHANGED_PRESENT -> Optional.of(message("notification.detect-conventional-config.unchanged-present"));
            case UNCHANGED_ABSENT -> Optional.of(message("notification.detect-conventional-config.unchanged-absent"));
            case NO_PROJECT_DIRECTORY -> Optional.empty();
        };
    }
}
