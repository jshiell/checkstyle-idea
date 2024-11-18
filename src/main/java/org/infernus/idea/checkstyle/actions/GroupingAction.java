package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareToggleAction;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.toolwindow.ResultGrouping;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static org.infernus.idea.checkstyle.actions.ToolWindowAccess.*;

abstract class GroupingAction extends DumbAwareToggleAction {

    private final ResultGrouping grouping;

    GroupingAction(@NotNull final ResultGrouping grouping) {
        this.grouping = grouping;
    }

    @Override
    public boolean isSelected(final @NotNull AnActionEvent event) {
        final Project project = getEventProject(event);
        if (project == null) {
            return false;
        }

        Boolean selected = getFromToolWindowPanel(toolWindow(project), panel -> panel.groupedBy() == grouping);
        return Objects.requireNonNullElse(selected, false);
    }

    @Override
    public void setSelected(final @NotNull AnActionEvent event, final boolean selected) {
        final Project project = getEventProject(event);
        if (project == null) {
            return;
        }

        actOnToolWindowPanel(toolWindow(project), panel -> {
            panel.groupBy(grouping);
        });
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
