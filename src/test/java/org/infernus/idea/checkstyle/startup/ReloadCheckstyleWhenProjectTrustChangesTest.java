package org.infernus.idea.checkstyle.startup;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.ide.trustedProjects.TrustedProjectsListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.util.ThreeState;
import com.intellij.util.messages.MessageBusConnection;
import kotlin.coroutines.Continuation;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.ConfigurationInvalidator;
import org.infernus.idea.checkstyle.config.PluginConfiguration;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class ReloadCheckstyleWhenProjectTrustChangesTest {

    private Project project;
    private Project otherProject;
    private ConfigurationInvalidator invalidator;
    private DaemonCodeAnalyzer daemonCodeAnalyzer;
    private CheckstyleProjectService checkstyleProjectService;
    private PluginConfiguration pluginConfig;
    private MessageBusConnection connection;
    private ReloadCheckstyleWhenProjectTrustChanges.Warner warner;
    private MockedStatic<ApplicationManager> applicationManager;
    private ThreeState trustState;

    @BeforeEach
    void setUp() {
        project = mock(Project.class);
        otherProject = mock(Project.class);
        invalidator = mock(ConfigurationInvalidator.class);
        daemonCodeAnalyzer = mock(DaemonCodeAnalyzer.class);
        checkstyleProjectService = mock(CheckstyleProjectService.class);
        connection = mock(MessageBusConnection.class);
        warner = mock(ReloadCheckstyleWhenProjectTrustChanges.Warner.class);
        trustState = ThreeState.YES;

        pluginConfig = mock(PluginConfiguration.class);
        when(pluginConfig.getThirdPartyClasspath()).thenReturn(List.of());
        PluginConfigurationManager configManager = mock(PluginConfigurationManager.class);
        when(configManager.getCurrent()).thenReturn(pluginConfig);

        when(project.getService(PluginConfigurationManager.class)).thenReturn(configManager);
        when(project.getService(ConfigurationInvalidator.class)).thenReturn(invalidator);
        when(project.getService(DaemonCodeAnalyzer.class)).thenReturn(daemonCodeAnalyzer);
        when(project.getService(CheckstyleProjectService.class)).thenReturn(checkstyleProjectService);

        // Scoped and thread-local, so it never installs a global application the way setApplication would.
        Application application = mock(Application.class);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(application).invokeLater(any(Runnable.class), any(Condition.class));
        applicationManager = mockStatic(ApplicationManager.class);
        applicationManager.when(ApplicationManager::getApplication).thenReturn(application);
    }

    @AfterEach
    void tearDown() {
        applicationManager.close();
    }

    private TrustedProjectsListener executeAndCaptureListener() {
        ReloadCheckstyleWhenProjectTrustChanges activity = new ReloadCheckstyleWhenProjectTrustChanges(
                parentDisposable -> connection, warner, Runnable::run, p -> trustState);
        activity.execute(project, mock(Continuation.class));

        ArgumentCaptor<TrustedProjectsListener> listener = ArgumentCaptor.forClass(TrustedProjectsListener.class);
        verify(connection).subscribe(eq(TrustedProjectsListener.TOPIC), listener.capture());
        return listener.getValue();
    }

    @Test
    void trustingThisProjectInvalidatesCachedResources() {
        TrustedProjectsListener listener = executeAndCaptureListener();

        listener.onProjectTrusted(project);

        verify(invalidator).invalidateCachedResources();
    }

    @Test
    void untrustingThisProjectInvalidatesCachedResources() {
        TrustedProjectsListener listener = executeAndCaptureListener();

        listener.onProjectUntrusted(project);

        verify(invalidator).invalidateCachedResources();
    }
}
