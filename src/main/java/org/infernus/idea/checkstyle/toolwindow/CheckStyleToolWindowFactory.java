package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.ui.content.Content;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.jetbrains.annotations.NotNull;

public class CheckStyleToolWindowFactory implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
        final Content toolContent = toolWindow.getContentManager().getFactory().createContent(
                new CheckStyleToolWindowPanel(toolWindow, project),
                CheckStyleBundle.message("plugin.toolwindow.action"),
                false);
        toolWindow.getContentManager().addContent(toolContent);

        toolWindow.setTitle(CheckStyleBundle.message("plugin.toolwindow.name"));
        toolWindow.setType(ToolWindowType.DOCKED, null);
    }

}
