package org.infernus.idea.checkstyle.startup;

import com.intellij.notification.NotificationAction;
import com.intellij.openapi.project.Project;
import kotlin.coroutines.Continuation;
import org.infernus.idea.checkstyle.CheckstyleArtifactDownloader;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.VersionListReader;
import org.infernus.idea.checkstyle.config.PluginConfiguration;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class PromptForMissingCheckstyleVersionTest {

    private Project project;
    private VersionListReader versionListReader;
    private PromptForMissingCheckstyleVersion.Notifier notifier;
    private PromptForMissingCheckstyleVersion activity;
    private CheckstyleArtifactDownloader downloader;
    private CheckstyleProjectService projectService;

    @BeforeEach
    void setUp() {
        project = mock(Project.class);
        versionListReader = mock(VersionListReader.class);
        notifier = mock(PromptForMissingCheckstyleVersion.Notifier.class);
        downloader = mock(CheckstyleArtifactDownloader.class);

        PluginConfigurationManager configManager = mock(PluginConfigurationManager.class);
        projectService = mock(CheckstyleProjectService.class);
        PluginConfiguration pluginConfig = mock(PluginConfiguration.class);

        when(project.getService(PluginConfigurationManager.class)).thenReturn(configManager);
        when(project.getService(CheckstyleProjectService.class)).thenReturn(projectService);
        when(configManager.getCurrent()).thenReturn(pluginConfig);
        when(pluginConfig.getCheckstyleVersion()).thenReturn("10.21.0");

        activity = new PromptForMissingCheckstyleVersion(versionListReader, notifier);
    }

    @Test
    void bundledVersion_noNotification() {
        when(versionListReader.isBundled("10.21.0")).thenReturn(true);

        activity.execute(project, mock(Continuation.class));

        verify(notifier, never()).showInfo(any(), any(), any());
    }

    @Test
    void locallyAvailable_noNotification() {
        when(versionListReader.isBundled("10.21.0")).thenReturn(false);
        when(projectService.getDownloader()).thenReturn(downloader);
        when(downloader.isAvailableLocally("10.21.0")).thenReturn(true);

        activity.execute(project, mock(Continuation.class));

        verify(notifier, never()).showInfo(any(), any(), any());
    }

    @Test
    void nullDownloader_noNotification() {
        when(versionListReader.isBundled("10.21.0")).thenReturn(false);
        when(projectService.getDownloader()).thenReturn(null);

        activity.execute(project, mock(Continuation.class));

        verify(notifier, never()).showInfo(any(), any(), any());
    }

    @Test
    void nonBundledNotLocal_showsNotification() {
        when(versionListReader.isBundled("10.21.0")).thenReturn(false);
        when(projectService.getDownloader()).thenReturn(downloader);
        when(downloader.isAvailableLocally("10.21.0")).thenReturn(false);

        activity.execute(project, mock(Continuation.class));

        verify(notifier).showInfo(eq(project), any(String.class), any(NotificationAction.class));
    }
}
