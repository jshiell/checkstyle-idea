package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareToggleAction;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static org.infernus.idea.checkstyle.actions.ToolWindowAccess.*;

/**
 * Action to toggle error display in tool window.
 */
public class DisplayErrors extends DumbAwareToggleAction {

    @Override
    public boolean isSelected(final @NotNull AnActionEvent event) {
        final Project project = getEventProject(event);
        if (project == null) {
            return false;
        }

        Boolean displayingErrors = getFromToolWindowPanel(toolWindow(project), CheckStyleToolWindowPanel::isDisplayingErrors);
        return Objects.requireNonNullElse(displayingErrors, false);
    }

    @Override
    public void setSelected(final @NotNull AnActionEvent event, final boolean selected) {
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
