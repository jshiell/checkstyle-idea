package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.StaticScanner;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static java.util.Optional.ofNullable;
import static org.infernus.idea.checkstyle.actions.ToolWindowAccess.actOnToolWindowPanel;
import static org.infernus.idea.checkstyle.actions.ToolWindowAccess.getFromToolWindowPanel;

/**
 * Base class for plug-in actions.
 */
public abstract class BaseAction extends DumbAwareAction {

    private static final Logger LOG = Logger.getInstance(BaseAction.class);

    @Override
    public void update(final AnActionEvent event) {
        try {
            final Project project = getEventProject(event);
            final Presentation presentation = event.getPresentation();

            if (project == null) {
                presentation.setEnabled(false);
                presentation.setVisible(false);
                return;
            }

            final ToolWindow toolWindow = ToolWindowAccess.toolWindow(project);
            if (toolWindow == null) {
                presentation.setEnabled(false);
                presentation.setVisible(false);

                return;
            }

            presentation.setEnabled(toolWindow.isAvailable());
            presentation.setVisible(true);

        } catch (Throwable e) {
            LOG.warn("Action update failed", e);
        }
    }

    protected void setProgressText(final ToolWindow toolWindow, final String progressTextKey) {
        actOnToolWindowPanel(toolWindow, panel -> panel.setProgressText(CheckStyleBundle.message(progressTextKey)));
    }

    @Nullable
    protected ConfigurationLocation getSelectedOverride(final ToolWindow toolWindow) {
        return getFromToolWindowPanel(toolWindow, CheckStyleToolWindowPanel::getSelectedOverride);
    }

    protected Optional<Project> project(@NotNull final AnActionEvent event) {
        return ofNullable(getEventProject(event));
    }

    protected PluginConfigurationManager configurationManager(final Project project) {
        return ServiceManager.getService(project, PluginConfigurationManager.class);
    }

    protected StaticScanner staticScanner(final Project project) {
        return ServiceManager.getService(project, StaticScanner.class);
    }

    protected boolean containsAtLeastOneFile(@NotNull final VirtualFile... files) {
        boolean result = false;
        for (VirtualFile file : files) {
            if ((file.isDirectory() && containsAtLeastOneFile(file.getChildren())) || (!file.isDirectory() && file
                    .isValid())) {
                result = true;
                break;
            }
        }
        return result;
    }
}
