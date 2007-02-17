package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.CheckStyleConfiguration;
import org.infernus.idea.checkstyle.toolwindow.ToolWindowPanel;

/**
 * Toggle the scroll to source setting.
 *
 * @author James Shiell
 * @version 1.0
 */
public final class ScrollToSource extends ToggleAction {

    /**
     * {@inheritDoc}
     */
    public boolean isSelected(final AnActionEvent event) {
        final Project project = (Project) event.getDataContext().getData(
                DataConstants.PROJECT);
        if (project == null) {
            return false;
        }

        final CheckStylePlugin checkStylePlugin
                = project.getComponent(CheckStylePlugin.class);
        if (checkStylePlugin == null) {
            throw new IllegalStateException("Couldn't get checkstyle plugin");
        }

        final ToolWindow toolWindow = ToolWindowManager.getInstance(
                project).getToolWindow(checkStylePlugin.getToolWindowId());

        // toggle value
        final ToolWindowPanel panel = (ToolWindowPanel)
                toolWindow.getComponent();
        return panel.isScrollToSource();
    }

    /**
     * {@inheritDoc}
     */
    public void setSelected(final AnActionEvent event, final boolean selected) {
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

        // toggle value
        final ToolWindowPanel panel = (ToolWindowPanel)
                toolWindow.getComponent();
        panel.setScrollToSource(selected);

    }
}
