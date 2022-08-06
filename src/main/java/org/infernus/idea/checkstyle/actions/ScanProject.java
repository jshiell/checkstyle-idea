package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static org.infernus.idea.checkstyle.actions.ToolWindowAccess.toolWindow;

/**
 * Action to execute a CheckStyle scan on the current project.
 */
public class ScanProject extends BaseAction {
    private static final Logger LOG = Logger.getInstance(ScanProject.class);

    @Override
    public void actionPerformed(final @NotNull AnActionEvent event) {
        project(event).ifPresent(project -> {
            try {
                final ScanScope scope = configurationManager(project).getCurrent().getScanScope();

                final ToolWindow toolWindow = toolWindow(project);
                toolWindow.activate(() -> executeScan(project, scope, toolWindow));

            } catch (Throwable e) {
                LOG.warn("Project scan failed", e);
            }
        });
    }

    private void executeScan(final Project project, final ScanScope scope, final ToolWindow toolWindow) {
        try {
            setProgressText(toolWindow, "plugin.status.in-progress.project");
            Runnable scanAction = null;
            if (scope == ScanScope.Everything) {
                scanAction = new ScanAllFilesInProjectTask(project, getSelectedOverride(toolWindow));
            } else {
                final ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);
                final VirtualFile[] sourceRoots = projectRootManager.getContentSourceRoots();
                if (sourceRoots.length > 0) {
                    scanAction = new ScanAllGivenFilesTask(project, sourceRoots, getSelectedOverride(toolWindow));
                }
            }
            if (scanAction != null) {
                ApplicationManager.getApplication().runReadAction(scanAction);
            }
        } catch (Throwable e) {
            LOG.warn("Project scan failed", e);
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent event) {
        super.update(event);

        try {
            final Presentation presentation = event.getPresentation();

            Optional<Project> projectFromEvent = project(event);
            if (projectFromEvent.isEmpty()) {
                presentation.setEnabled(false);
                return;
            }

            projectFromEvent.ifPresent(project -> {
                final ScanScope scope = configurationManager(project).getCurrent().getScanScope();

                final ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);
                VirtualFile[] sourceRoots;
                if (scope == ScanScope.Everything) {
                    sourceRoots = projectRootManager.getContentRoots();
                } else {
                    sourceRoots = projectRootManager.getContentSourceRoots();
                }

                // disable if no files are selected or scan in progress
                if (containsAtLeastOneFile(sourceRoots)) {
                    presentation.setEnabled(!staticScanner(project).isScanInProgress());
                } else {
                    presentation.setEnabled(false);
                }
            });
        } catch (Throwable e) {
            LOG.warn("Project button update failed", e);
        }
    }

}
