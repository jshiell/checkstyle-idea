package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.toolwindow.ToolWindowPanel;

/**
 * Action to collapse all nodes in the results window.
 *
 * @author James Shiell
 * @version 1.0
 */
public class CollapseAll extends BaseAction {

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(final AnActionEvent event) {
        final Project project = (Project) event.getDataContext().getData(
                DataConstants.PROJECT);
        if (project == null) {
            return;
        }

        final CheckStylePlugin checkStylePlugin
                = project.getComponent(CheckStylePlugin.class);
        if (checkStylePlugin == null) {
            throw new IllegalStateException("Couldn't get checkstyle plugin");
        }

        final ToolWindow toolWindow = ToolWindowManager.getInstance(
                project).getToolWindow(checkStylePlugin.getToolWindowId());

        final ToolWindowPanel panel = (ToolWindowPanel)
                toolWindow.getComponent();
        panel.collapseTree();
    }

}
