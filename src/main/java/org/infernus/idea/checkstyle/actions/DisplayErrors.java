package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;

import static org.infernus.idea.checkstyle.actions.ToolWindowAccess.*;

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

        Boolean displayingErrors = getFromToolWindowPanel(toolWindow(project), CheckStyleToolWindowPanel::isDisplayingErrors);
        if (displayingErrors != null) {
            return displayingErrors;
        }
        return false;
    }

    @Override
    public void setSelected(final AnActionEvent event, final boolean selected) {
        final Project project = getEventProject(event);
        if (project == null) {
            return;
        }

        actOnToolWindowPanel(toolWindow(project), panel -> {
            panel.setDisplayingErrors(selected);
            panel.filterDisplayedResults();
        });
    }
}
