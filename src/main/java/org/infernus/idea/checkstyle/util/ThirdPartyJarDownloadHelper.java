package org.infernus.idea.checkstyle.util;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.infernus.idea.checkstyle.ThirdPartyJarCache;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static org.infernus.idea.checkstyle.CheckStyleBundle.message;

/**
 * Downloads (or re-downloads) a third-party check JAR into {@link ThirdPartyJarCache} with a modal
 * progress indicator, offering Retry/Cancel on failure. Mirrors
 * {@link CheckstyleDownloadHelper#downloadWithProgress}, but simpler: there is no "use bundled
 * version" fallback branch here, since it does not apply to a user-supplied third-party JAR URL.
 * <p>
 * Runs only {@link ThirdPartyJarCache#forceRefresh} inside {@link Task.Modal#run}; the caller must
 * not mutate any Swing model until after this method returns, to avoid a background-thread EDT
 * violation.
 */
public final class ThirdPartyJarDownloadHelper {

    private ThirdPartyJarDownloadHelper() {
    }

    public static boolean forceRefreshWithProgress(@NotNull final Project project,
                                                    @NotNull final String url,
                                                    @NotNull final ThirdPartyJarCache cache) {
        final String title = message("config.path.download.title", url);
        while (true) {
            final Throwable[] failure = {null};
            ProgressManager.getInstance().run(new Task.Modal(project, title, true) {
                @Override
                public void run(@NotNull final ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    indicator.setText(message("config.path.download.progress", url));
                    try {
                        cache.forceRefresh(url);
                    } catch (Exception e) {
                        failure[0] = e;
                    }
                }
            });

            if (failure[0] == null) {
                return true;
            }

            final String errorText = Objects.toString(failure[0].getMessage(), failure[0].getClass().getSimpleName());
            final String msg = message("config.path.download.failed", url, errorText);
            final int choice = Messages.showDialog(project, msg, title, new String[]{"Retry", "Cancel"}, 0, Messages.getErrorIcon());
            if (choice == 0) {
                continue;
            }
            return false;
        }
    }
}
