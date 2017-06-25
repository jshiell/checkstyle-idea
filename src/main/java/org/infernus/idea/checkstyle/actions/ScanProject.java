package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;

/**
 * Action to execute a CheckStyle scan on the current project.
 */
public class ScanProject extends BaseAction {

    @Override
    public void actionPerformed(final AnActionEvent event) {
        try {
            final Project project = DataKeys.PROJECT.getData(event.getDataContext());
            if (project == null) {
                return;
            }

            final CheckStylePlugin checkStylePlugin
                    = project.getComponent(CheckStylePlugin.class);
            if (checkStylePlugin == null) {
                throw new IllegalStateException("Couldn't get checkstyle plugin");
            }
            final ScanScope scope = checkStylePlugin.getConfiguration().getCurrentPluginConfig().getScanScope();

            final ToolWindow toolWindow = ToolWindowManager.getInstance(
                    project).getToolWindow(CheckStyleToolWindowPanel.ID_TOOLWINDOW);
            toolWindow.activate(() -> {
                try {
                    setProgressText(toolWindow, "plugin.status.in-progress.project");
                    Runnable scanAction = null;
                    if (scope == ScanScope.Everything) {
                        scanAction = new ScanEverythingAction(project, getSelectedOverride(toolWindow));
                    } else {
                        final ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);
                        final VirtualFile[] sourceRoots = projectRootManager.getContentSourceRoots();
                        if (sourceRoots.length > 0) {
                            scanAction = new ScanSourceRootsAction(project, sourceRoots, getSelectedOverride(toolWindow));
                        }
                    }
                    if (scanAction != null) {
                        ApplicationManager.getApplication().runReadAction(scanAction);
                    }
                } catch (Throwable e) {
                    CheckStylePlugin.processErrorAndLog("Project scan", e);
                }
            });

        } catch (Throwable e) {
            CheckStylePlugin.processErrorAndLog("Project scan", e);
        }
    }

    @Override
    public void update(final AnActionEvent event) {
        super.update(event);

        try {
            final Presentation presentation = event.getPresentation();

            final Project project = DataKeys.PROJECT.getData(event.getDataContext());
            if (project == null) { // check if we're loading...
                presentation.setEnabled(false);
                return;
            }

            final CheckStylePlugin checkStylePlugin = project.getComponent(CheckStylePlugin.class);
            if (checkStylePlugin == null) {
                throw new IllegalStateException("Couldn't get checkstyle plugin");
            }
            final ScanScope scope = checkStylePlugin.getConfiguration().getCurrentPluginConfig().getScanScope();

            VirtualFile[] sourceRoots = null;
            if (scope == ScanScope.Everything) {
                sourceRoots = new VirtualFile[]{project.getBaseDir()};
            } else {
                final ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);
                sourceRoots = projectRootManager.getContentSourceRoots();
            }

            // disable if no files are selected or scan in progress
            if (containsAtLeastOneFile(sourceRoots)) {
                presentation.setEnabled(!checkStylePlugin.isScanInProgress());
            } else {
                presentation.setEnabled(false);
            }
        } catch (Throwable e) {
            CheckStylePlugin.processErrorAndLog("Project button update", e);
        }
    }

}
