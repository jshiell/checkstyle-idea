package org.infernus.idea.checkstyle.startup;

import com.intellij.notification.NotificationAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.infernus.idea.checkstyle.CheckstyleArtifactDownloader;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.VersionListReader;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.util.CheckstyleDownloadHelper;
import org.infernus.idea.checkstyle.util.Notifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.infernus.idea.checkstyle.CheckStyleBundle.message;

public class PromptForMissingCheckstyleVersion implements ProjectActivity {

    @FunctionalInterface
    interface Notifier {
        void showInfo(Project project, String text, NotificationAction action);
    }

    private final VersionListReader versionListReader;
    private final Notifier notifier;

    public PromptForMissingCheckstyleVersion() {
        this(new VersionListReader(), Notifications::showInfo);
    }

    PromptForMissingCheckstyleVersion(@NotNull final VersionListReader versionListReader,
                                      @NotNull final Notifier notifier) {
        this.versionListReader = versionListReader;
        this.notifier = notifier;
    }

    @Nullable
    @Override
    public Object execute(@NotNull final Project project,
                          @NotNull final Continuation<? super Unit> continuation) {
        String version = project.getService(PluginConfigurationManager.class).getCurrent().getCheckstyleVersion();

        if (versionListReader.isBundled(version)) {
            return null;
        }

        CheckstyleArtifactDownloader downloader = project.getService(CheckstyleProjectService.class).getDownloader();
        if (downloader == null) {
            return null;
        }
        if (downloader.isAvailableLocally(version)) {
            return null;
        }

        notifier.showInfo(project,
                message("startup.download.prompt", version),
                NotificationAction.createSimple(
                        message("startup.download.action"),
                        () -> {
                            if (!project.isDisposed()) {
                                CheckstyleDownloadHelper.downloadWithProgress(project, version, downloader, versionListReader, null);
                            }
                        }
                )
        );
        return null;
    }
}
