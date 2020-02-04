package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;

import static org.infernus.idea.checkstyle.actions.ToolWindowAccess.toolWindow;

/**
 * Action to close the tool window.
 */
public class Close extends BaseAction {

    @Override
    public void actionPerformed(final AnActionEvent event) {
        project(event).ifPresent(project -> toolWindow(project).hide(null));
    }

}
