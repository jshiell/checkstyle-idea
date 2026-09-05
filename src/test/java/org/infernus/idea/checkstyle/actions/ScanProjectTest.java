package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;
import org.infernus.idea.checkstyle.util.Async;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ScanProjectTest {

    @Test
    void noSourceRootsForANonEverythingScopeReportsAWarningAndDispatchesNoScan() {
        final Project project = mock(Project.class);
        final ToolWindow toolWindow = mock(ToolWindow.class);
        final ProjectRootManager projectRootManager = mock(ProjectRootManager.class);
        when(projectRootManager.getContentSourceRoots()).thenReturn(new VirtualFile[0]);
        final CheckStyleToolWindowPanel panel = mock(CheckStyleToolWindowPanel.class);

        try (MockedStatic<ProjectRootManager> projectRootManagerStatic = mockStatic(ProjectRootManager.class);
             MockedStatic<CheckStyleToolWindowPanel> panelStatic = mockStatic(CheckStyleToolWindowPanel.class);
             MockedStatic<Async> asyncStatic = mockStatic(Async.class)) {
            projectRootManagerStatic.when(() -> ProjectRootManager.getInstance(project)).thenReturn(projectRootManager);
            panelStatic.when(() -> CheckStyleToolWindowPanel.panelFor(project)).thenReturn(panel);

            new ScanProject().executeScan(project, ScanScope.AllSources, toolWindow);

            verify(panel).displayWarningResult("plugin.status.in-progress.no-project-source-roots");
            asyncStatic.verify(() -> Async.executeOnPooledThread(any()), never());
            verifyNoInteractions(toolWindow);
        }
    }

    @Test
    void nonEmptySourceRootsDispatchesAScanAndSetsTheProgressText() {
        final Project project = mock(Project.class);
        final ToolWindow toolWindow = mock(ToolWindow.class);
        final ContentManager contentManager = mock(ContentManager.class);
        final Content content = mock(Content.class);
        final CheckStyleToolWindowPanel panel = mock(CheckStyleToolWindowPanel.class);
        when(toolWindow.getContentManager()).thenReturn(contentManager);
        when(contentManager.getContent(0)).thenReturn(content);
        when(content.getComponent()).thenReturn(panel);

        final ProjectRootManager projectRootManager = mock(ProjectRootManager.class);
        when(projectRootManager.getContentSourceRoots()).thenReturn(new VirtualFile[]{mock(VirtualFile.class)});

        try (MockedStatic<ProjectRootManager> projectRootManagerStatic = mockStatic(ProjectRootManager.class);
             MockedStatic<CheckStyleToolWindowPanel> panelStatic = mockStatic(CheckStyleToolWindowPanel.class);
             MockedStatic<Async> asyncStatic = mockStatic(Async.class)) {
            projectRootManagerStatic.when(() -> ProjectRootManager.getInstance(project)).thenReturn(projectRootManager);
            panelStatic.when(() -> CheckStyleToolWindowPanel.panelFor(project)).thenReturn(panel);

            new ScanProject().executeScan(project, ScanScope.AllSources, toolWindow);

            verify(panel).setProgressText(CheckStyleBundle.message("plugin.status.in-progress.project"));
            asyncStatic.verify(() -> Async.executeOnPooledThread(any()));
            verify(panel, never()).displayWarningResult(any());
        }
    }
}
