package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.infernus.idea.checkstyle.CheckStyleConstants;

/**
 * Base class for plug-in actions.
 *
 * @author James Shiell
 * @version 1.0
 */
public abstract class BaseAction extends AnAction {

    /**
     * {@inheritDoc}
     */
    public void update(final AnActionEvent event) {
        final Project project = (Project) event.getDataContext().getData(
                DataConstants.PROJECT);
        final Presentation presentation = event.getPresentation();

        // check a project is loaded
        if (project == null) {
            presentation.setEnabled(false);
            presentation.setVisible(false);

            return;

        }

        // check if tool window is registered
        final ToolWindow toolWindow = ToolWindowManager.getInstance(
                project).getToolWindow(CheckStyleConstants.ID_TOOL_WINDOW);
        if (toolWindow == null) {
            presentation.setEnabled(false);
            presentation.setVisible(false);

            return;
        }

        // enable
        presentation.setEnabled(toolWindow.isAvailable());
        presentation.setVisible(true);
    }

}
