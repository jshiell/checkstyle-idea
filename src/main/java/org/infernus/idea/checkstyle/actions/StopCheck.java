package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;

/**
 * Action to stop a check in progress.
 */
public class StopCheck extends BaseAction {

    @Override
    public void actionPerformed(final AnActionEvent event) {
        project(event).ifPresent(project -> {
            try {
                final ToolWindow toolWindow = ToolWindowManager
                        .getInstance(project)
                        .getToolWindow(CheckStyleToolWindowPanel.ID_TOOLWINDOW);
                toolWindow.activate(() -> {
                    setProgressText(toolWindow, "plugin.status.in-progress.current");

                    plugin(project).stopChecks();

                    setProgressText(toolWindow, "plugin.status.aborted");
                });

            } catch (Throwable e) {
                CheckStylePlugin.processErrorAndLog("Abort Scan", e);
            }
        });
    }

    @Override
    public void update(final AnActionEvent event) {
        super.update(event);

        project(event).ifPresent(project -> {
            try {
                final Presentation presentation = event.getPresentation();
                presentation.setEnabled(plugin(project).isScanInProgress());

            } catch (Throwable e) {
                CheckStylePlugin.processErrorAndLog("Abort button update", e);
            }
        });
    }
}
