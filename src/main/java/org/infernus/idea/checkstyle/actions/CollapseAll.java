package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;

/**
 * Action to collapse all nodes in the results window.
 */
public class CollapseAll extends BaseAction {

    @Override
    public void actionPerformed(final AnActionEvent event) {
        project(event).ifPresent(project -> {
            final ToolWindow toolWindow = ToolWindowManager
                    .getInstance(project)
                    .getToolWindow(CheckStyleToolWindowPanel.ID_TOOLWINDOW);

            final Content content = toolWindow.getContentManager().getContent(0);
            if (content != null && content.getComponent() instanceof CheckStyleToolWindowPanel) {
                ((CheckStyleToolWindowPanel) content.getComponent()).collapseTree();
            }
        });
    }

}
