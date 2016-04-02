package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Optional;

import static com.intellij.openapi.actionSystem.CommonDataKeys.PROJECT;
import static java.util.Optional.ofNullable;

/**
 * Base class for plug-in actions.
 */
public abstract class BaseAction extends AnAction {

    private static final Log LOG = LogFactory.getLog(
            BaseAction.class);

    @Override
    public void update(final AnActionEvent event) {
        try {
            final Project project = DataKeys.PROJECT.getData(event.getDataContext());
            final Presentation presentation = event.getPresentation();

            // check a project is loaded
            if (project == null) {
                presentation.setEnabled(false);
                presentation.setVisible(false);

                return;
            }

            final CheckStylePlugin checkStylePlugin
                    = project.getComponent(CheckStylePlugin.class);
            if (checkStylePlugin == null) {
                throw new IllegalStateException("Couldn't get checkstyle plugin");
            }

            // check if tool window is registered
            final ToolWindow toolWindow = ToolWindowManager.getInstance(
                    project).getToolWindow(CheckStyleToolWindowPanel.ID_TOOLWINDOW);
            if (toolWindow == null) {
                presentation.setEnabled(false);
                presentation.setVisible(false);

                return;
            }

            // enable
            presentation.setEnabled(toolWindow.isAvailable());
            presentation.setVisible(true);

        } catch (Throwable e) {
            final CheckStylePluginException processed
                    = CheckStylePluginException.wrap(e);
            if (processed != null) {
                LOG.error("Action update failed", processed);
            }
        }
    }

    protected void setProgressText(final ToolWindow toolWindow, final String progressTextKey) {
        final Content content = toolWindow.getContentManager().getContent(0);
        if (content != null) {
            final JComponent component = content.getComponent();
            // the content instance will be a JLabel while the component initialises
            if (component instanceof CheckStyleToolWindowPanel) {
                final CheckStyleToolWindowPanel panel = (CheckStyleToolWindowPanel) component;
                panel.setProgressText(CheckStyleBundle.message(progressTextKey));
            }
        }
    }

    protected ConfigurationLocation getSelectedOverride(final ToolWindow toolWindow) {
        final Content content = toolWindow.getContentManager().getContent(0);
        // the content instance will be a JLabel while the component initialises
        if (content != null && content.getComponent() instanceof CheckStyleToolWindowPanel) {
            return ((CheckStyleToolWindowPanel) content.getComponent()).getSelectedOverride();
        }
        return null;
    }

    protected Optional<Project> project(@NotNull final AnActionEvent event) {
        return ofNullable(PROJECT.getData(event.getDataContext()));
    }
}
