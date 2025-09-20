package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;
import org.jetbrains.annotations.NotNull;

import static org.infernus.idea.checkstyle.actions.ToolWindowAccess.*;

public class NextResult extends BaseAction {

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        project(event).ifPresent(project -> {
            if (isFocusInToolWindow(project)) {
                actOnToolWindowPanel(toolWindow(project), CheckStyleToolWindowPanel::selectNextResult);
            }
        });
    }

}
