package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import static org.infernus.idea.checkstyle.actions.ToolWindowAccess.toolWindow;

/**
 * Action to close the tool window.
 */
public class Close extends BaseAction {

    @Override
    public void actionPerformed(final @NotNull AnActionEvent event) {
        project(event).ifPresent(project -> toolWindow(project).hide(null));
    }

}
