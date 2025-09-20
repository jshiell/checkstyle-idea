package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ToolWindowAccess {
    private ToolWindowAccess() {
    }

    static void actOnToolWindowPanel(final ToolWindow toolWindow, final Consumer<CheckStyleToolWindowPanel> action) {
        final Content content = toolWindow.getContentManager().getContent(0);
        // the content instance will be a JLabel while the component initialises
        if (content != null && content.getComponent() instanceof CheckStyleToolWindowPanel) {
            action.accept((CheckStyleToolWindowPanel) content.getComponent());
        }
    }

    static <R> R getFromToolWindowPanel(final ToolWindow toolWindow, final Function<CheckStyleToolWindowPanel, R> action) {
        final Content content = toolWindow.getContentManager().getContent(0);
        // the content instance will be a JLabel while the component initialises
        if (content != null && content.getComponent() instanceof CheckStyleToolWindowPanel) {
            return action.apply((CheckStyleToolWindowPanel) content.getComponent());
        }
        return null;
    }

    static ToolWindow toolWindow(final Project project) {
        return ToolWindowManager
                .getInstance(project)
                .getToolWindow(CheckStyleToolWindowPanel.ID_TOOLWINDOW);
    }

    static boolean isFocusInToolWindow(@NotNull final Project project) {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(CheckStyleToolWindowPanel.ID_TOOLWINDOW);
        if (toolWindow == null || !toolWindow.isVisible()) {
            return false;
        }
        final Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, toolWindow.getComponent());
    }
}
