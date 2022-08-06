package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.wm.ToolWindow;
import org.jetbrains.annotations.NotNull;

import static org.infernus.idea.checkstyle.actions.ToolWindowAccess.toolWindow;

/**
 * Action to stop a check in progress.
 */
public class StopCheck extends BaseAction {
    private static final Logger LOG = Logger.getInstance(StopCheck.class);

    @Override
    public void actionPerformed(final @NotNull AnActionEvent event) {
        project(event).ifPresent(project -> {
            try {
                final ToolWindow toolWindow = toolWindow(project);
                toolWindow.activate(() -> {
                    setProgressText(toolWindow, "plugin.status.in-progress.current");

                    staticScanner(project).stopChecks();

                    setProgressText(toolWindow, "plugin.status.aborted");
                });

            } catch (Throwable e) {
                LOG.warn("Abort Scan failed", e);
            }
        });
    }

    @Override
    public void update(final @NotNull AnActionEvent event) {
        super.update(event);

        project(event).ifPresent(project -> {
            try {
                final Presentation presentation = event.getPresentation();
                presentation.setEnabled(staticScanner(project).isScanInProgress());

            } catch (Throwable e) {
                LOG.warn("Abort button update failed", e);
            }
        });
    }
}
