package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;
import org.jetbrains.annotations.NotNull;

import static org.infernus.idea.checkstyle.actions.ToolWindowAccess.actOnToolWindowPanel;
import static org.infernus.idea.checkstyle.actions.ToolWindowAccess.toolWindow;

/**
 * Action to collapse all nodes in the results window.
 */
public class CollapseAll extends BaseAction {

    @Override
    public void actionPerformed(final @NotNull AnActionEvent event) {
        project(event).ifPresent(project -> actOnToolWindowPanel(toolWindow(project), CheckStyleToolWindowPanel::collapseTree));
    }

}
