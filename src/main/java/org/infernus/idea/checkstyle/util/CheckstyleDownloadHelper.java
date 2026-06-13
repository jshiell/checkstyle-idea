package org.infernus.idea.checkstyle.util;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.infernus.idea.checkstyle.CheckstyleArtifactDownloader;
import org.infernus.idea.checkstyle.VersionListReader;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static org.infernus.idea.checkstyle.CheckStyleBundle.message;

public final class CheckstyleDownloadHelper {

    private CheckstyleDownloadHelper() {
    }

    public static boolean downloadWithProgress(@NotNull final Project project,
                                               @NotNull final String version,
                                               @NotNull final CheckstyleArtifactDownloader downloader,
                                               @NotNull final VersionListReader versionListReader,
                                               @NotNull final Consumer<String> onVersionChanged) {
        String title = message("config.download.title", version);
        while (true) {
            final Throwable[] failure = {null};
            ProgressManager.getInstance().run(new Task.Modal(project, title, true) {
                @Override
                public void run(@NotNull final ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    indicator.setText(message("config.download.progress", version));
                    try {
                        downloader.download(version);
                    } catch (Exception e) {
                        failure[0] = e;
                    }
                }
            });

            if (failure[0] == null) {
                return true;
            }

            String msg = message("config.download.failed", version, failure[0].getMessage());
            int choice = Messages.showDialog(project, msg, title,
                    new String[]{"Retry", "Use bundled version", "Cancel"}, 0, Messages.getErrorIcon());
            if (choice == 0) {
                continue;
            }
            if (choice == 1) {
                String bundled = versionListReader.getBundledVersions().last();
                onVersionChanged.accept(bundled);
                return true;
            }
            return false;
        }
    }
}
