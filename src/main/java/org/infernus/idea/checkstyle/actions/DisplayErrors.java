package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;

/**
 * Action to toggle error display in tool window.
 */
public class DisplayErrors extends ToggleAction {

    @Override
    public boolean isSelected(final AnActionEvent event) {
        final Project project = getEventProject(event);
        if (project == null) {
            return false;
        }

        final ToolWindow toolWindow = ToolWindowManager
                .getInstance(project)
                .getToolWindow(CheckStyleToolWindowPanel.ID_TOOLWINDOW);

        final Content content = toolWindow.getContentManager().getContent(0);
        if (content != null && content.getComponent() instanceof CheckStyleToolWindowPanel) {
            return ((CheckStyleToolWindowPanel) content.getComponent()).isDisplayingErrors();
        }

        return false;
    }

    @Override
    public void setSelected(final AnActionEvent event, final boolean selected) {
        final Project project = getEventProject(event);
        if (project == null) {
            return;
        }

        final ToolWindow toolWindow = ToolWindowManager.getInstance(
                project).getToolWindow(CheckStyleToolWindowPanel.ID_TOOLWINDOW);

        final Content content = toolWindow.getContentManager().getContent(0);
        if (content != null && content.getComponent() instanceof CheckStyleToolWindowPanel) {
            final CheckStyleToolWindowPanel panel = (CheckStyleToolWindowPanel) content.getComponent();
            panel.setDisplayingErrors(selected);
            panel.filterDisplayedResults();
        }
    }
}
