package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareToggleAction;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;

import static org.infernus.idea.checkstyle.actions.ToolWindowAccess.*;

/**
 * Toggle the scroll to source setting.
 */
public final class ScrollToSource extends DumbAwareToggleAction {

    @Override
    public boolean isSelected(final AnActionEvent event) {
        final Project project = getEventProject(event);
        if (project == null) {
            return false;
        }

        Boolean scrollToSource = getFromToolWindowPanel(toolWindow(project), CheckStyleToolWindowPanel::isScrollToSource);
        if (scrollToSource != null) {
            return scrollToSource;
        }
        return false;
    }

    @Override
    public void setSelected(final AnActionEvent event, final boolean selected) {
        final Project project = getEventProject(event);
        if (project == null) {
            return;
        }

        actOnToolWindowPanel(toolWindow(project), panel -> panel.setScrollToSource(selected));
    }
}
