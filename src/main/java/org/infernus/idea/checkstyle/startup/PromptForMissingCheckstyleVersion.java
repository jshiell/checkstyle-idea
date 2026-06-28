package org.infernus.idea.checkstyle.startup;

import com.intellij.notification.NotificationAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.infernus.idea.checkstyle.CheckstyleArtifactDownloader;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.VersionListReader;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.util.CheckstyleDownloadHelper;
import org.infernus.idea.checkstyle.util.Notifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static org.infernus.idea.checkstyle.CheckStyleBundle.message;

public class PromptForMissingCheckstyleVersion implements ProjectActivity {

    @FunctionalInterface
    interface Notifier {
        void showInfo(Project project, String text, NotificationAction... actions);
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

    Consumer<String> buildOnVersionChanged(@NotNull final PluginConfigurationManager configManager) {
        return newVersion -> configManager.setCurrent(
                PluginConfigurationBuilder.from(configManager.getCurrent())
                        .withCheckstyleVersion(newVersion)
                        .build(),
                true);
    }

    @Nullable
    @Override
    public Object execute(@NotNull final Project project,
                          @NotNull final Continuation<? super Unit> continuation) {
        final PluginConfigurationManager configManager = project.getService(PluginConfigurationManager.class);
        final String configuredVersion = configManager.getCurrent().getCheckstyleVersion();
        final String version = versionListReader.isLatest(configuredVersion)
                ? versionListReader.getDefaultVersion()
                : configuredVersion;

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

        Consumer<String> onVersionChanged = buildOnVersionChanged(configManager);

        NotificationAction downloadAction = NotificationAction.createSimple(
                message("startup.download.action"),
                () -> {
                    if (!project.isDisposed()) {
                        CheckstyleDownloadHelper.downloadWithProgress(project, version, downloader, versionListReader, onVersionChanged);
                    }
                }
        );

        String bundledVersion = versionListReader.getBundledVersions().last();
        NotificationAction useBundledAction = NotificationAction.createSimple(
                message("startup.use-bundled.action", bundledVersion),
                () -> {
                    if (!project.isDisposed()) {
                        onVersionChanged.accept(bundledVersion);
                    }
                }
        );

        notifier.showInfo(project, message("startup.download.prompt", version), downloadAction, useBundledAction);
        return null;
    }
}
