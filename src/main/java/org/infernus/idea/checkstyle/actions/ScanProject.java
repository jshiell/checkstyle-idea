package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.concurrency.NonUrgentExecutor;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.jetbrains.annotations.NotNull;

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
            ThrowableRunnable<RuntimeException> scanAction = null;
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
                ReadAction.run(scanAction);
            }
        } catch (Throwable e) {
            LOG.warn("Project scan failed", e);
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent event) {
        final Presentation presentation = event.getPresentation();

        project(event).ifPresentOrElse(project -> ReadAction.nonBlocking(() -> {
                    try {
                        final ScanScope scope = configurationManager(project).getCurrent().getScanScope();

                        final ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);
                        if (scope == ScanScope.Everything) {
                            return projectRootManager.getContentRoots();
                        } else {
                            return projectRootManager.getContentSourceRoots();
                        }
                    } catch (Throwable e) {
                        LOG.warn("Project button update failed", e);
                        return VirtualFile.EMPTY_ARRAY;
                    }

                }).finishOnUiThread(ModalityState.any(), (sourceRoots) -> {
                    // disable if no files are selected or scan in progress
                    if (containsAtLeastOneFile(sourceRoots)) {
                        presentation.setEnabled(!staticScanner(project).isScanInProgress());
                    } else {
                        presentation.setEnabled(false);
                    }
                }).submit(NonUrgentExecutor.getInstance()),
                () -> presentation.setEnabled(false));
    }

}
