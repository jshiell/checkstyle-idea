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

import org.infernus.idea.checkstyle.model.ScanScope;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
    private PluginConfiguration pluginConfig;
    private PluginConfigurationManager configManager;

    @BeforeEach
    void setUp() {
        project = mock(Project.class);
        versionListReader = mock(VersionListReader.class);
        notifier = mock(PromptForMissingCheckstyleVersion.Notifier.class);
        downloader = mock(CheckstyleArtifactDownloader.class);

        configManager = mock(PluginConfigurationManager.class);
        projectService = mock(CheckstyleProjectService.class);
        pluginConfig = mock(PluginConfiguration.class);

        when(project.getService(PluginConfigurationManager.class)).thenReturn(configManager);
        when(project.getService(CheckstyleProjectService.class)).thenReturn(projectService);
        when(configManager.getCurrent()).thenReturn(pluginConfig);
        when(pluginConfig.getCheckstyleVersion()).thenReturn("10.21.0");
        when(pluginConfig.getScanScope()).thenReturn(ScanScope.JavaOnlyWithTests);
        when(pluginConfig.getLocations()).thenReturn(new TreeSet<>());
        when(pluginConfig.getThirdPartyClasspath()).thenReturn(new ArrayList<>());
        when(pluginConfig.getActiveLocationIds()).thenReturn(new TreeSet<>());

        TreeSet<String> bundled = new TreeSet<>();
        bundled.add("14.0.0");
        when(versionListReader.getBundledVersions()).thenReturn(bundled);

        activity = new PromptForMissingCheckstyleVersion(versionListReader, notifier);
    }

    @Test
    void bundledVersionNoNotification() {
        when(versionListReader.isBundled("10.21.0")).thenReturn(true);

        activity.execute(project, mock(Continuation.class));

        verifyNoInteractions(notifier);
    }

    @Test
    void locallyAvailableNoNotification() {
        when(versionListReader.isBundled("10.21.0")).thenReturn(false);
        when(projectService.getDownloader()).thenReturn(downloader);
        when(downloader.isAvailableLocally("10.21.0")).thenReturn(true);

        activity.execute(project, mock(Continuation.class));

        verifyNoInteractions(notifier);
    }

    @Test
    void nullDownloaderNoNotification() {
        when(versionListReader.isBundled("10.21.0")).thenReturn(false);
        when(projectService.getDownloader()).thenReturn(null);

        activity.execute(project, mock(Continuation.class));

        verifyNoInteractions(notifier);
    }

    @Test
    void latestVersionNoNotification() {
        when(pluginConfig.getCheckstyleVersion()).thenReturn("latest");
        when(versionListReader.isLatest("latest")).thenReturn(true);
        when(versionListReader.getDefaultVersion()).thenReturn("14.0.0");
        when(versionListReader.isBundled("14.0.0")).thenReturn(true);

        activity.execute(project, mock(Continuation.class));

        verifyNoInteractions(notifier);
    }

    @Test
    void nonBundledNotLocalShowsTwoActions() {
        when(versionListReader.isBundled("10.21.0")).thenReturn(false);
        when(projectService.getDownloader()).thenReturn(downloader);
        when(downloader.isAvailableLocally("10.21.0")).thenReturn(false);

        activity.execute(project, mock(Continuation.class));

        verify(notifier).showInfo(eq(project), any(String.class),
                any(NotificationAction.class), any(NotificationAction.class));
    }

    @Test
    void useBundledActionUpdatesCurrent() {
        Consumer<String> onVersionChanged = activity.buildOnVersionChanged(configManager);

        onVersionChanged.accept("14.0.0");

        verify(configManager).setCurrent(
                argThat(c -> "14.0.0".equals(c.getCheckstyleVersion())),
                eq(true));
    }
}
