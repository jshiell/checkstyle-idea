package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStyleConstants;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;

import java.util.ArrayList;
import java.util.List;

/**
 * Action to execute a CheckStyle scan on the current project.
 *
 * @author James Shiell
 * @version 1.0
 */
public class ScanProject extends BaseAction {

    /**
     * Logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(
            ScanProject.class);

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(final AnActionEvent event) {
        try {
            final Project project = DataKeys.PROJECT.getData(event.getDataContext());
            if (project == null) {
                return;
            }

            final CheckStylePlugin checkStylePlugin
                    = project.getComponent(CheckStylePlugin.class);
            if (checkStylePlugin == null) {
                throw new IllegalStateException("Couldn't get checkstyle plugin");
            }

            final ToolWindow toolWindow = ToolWindowManager.getInstance(
                    project).getToolWindow(CheckStyleConstants.ID_TOOLWINDOW);
            toolWindow.activate(null);

            setProgressText(toolWindow, "plugin.status.in-progress.project");

            // find project files
            final ProjectRootManager projectRootManager
                    = ProjectRootManager.getInstance(project);
            final VirtualFile[] sourceRoots
                    = projectRootManager.getContentSourceRoots();

            if (sourceRoots != null && sourceRoots.length > 0) {
                project.getComponent(CheckStylePlugin.class).checkFiles(
                        flattenFiles(sourceRoots), event);
            }

        } catch (Throwable e) {
            final CheckStylePluginException processed
                    = CheckStylePlugin.processError(null, e);
            if (processed != null) {
                LOG.error("Current project scan failed", processed);
            }
        }
    }

    /**
     * Flatten a nested list of files, returning all files in the array.
     *
     * @param files the top level of files.
     * @return the flattened list of files.
     */
    private List<VirtualFile> flattenFiles(final VirtualFile[] files) {
        final List<VirtualFile> flattened = new ArrayList<VirtualFile>();

        if (files != null) {
            for (final VirtualFile file : files) {
                flattened.add(file);

                if (file.getChildren() != null) {
                    flattened.addAll(flattenFiles(file.getChildren()));
                }
            }
        }

        return flattened;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final AnActionEvent event) {
        super.update(event);

        try {
            final Presentation presentation = event.getPresentation();

            final Project project = DataKeys.PROJECT.getData(event.getDataContext());
            if (project == null) { // check if we're loading...
                presentation.setEnabled(false);
                return;
            }

            final CheckStylePlugin checkStylePlugin
                    = project.getComponent(CheckStylePlugin.class);
            if (checkStylePlugin == null) {
                throw new IllegalStateException("Couldn't get checkstyle plugin");
            }

            final ProjectRootManager projectRootManager
                    = ProjectRootManager.getInstance(project);
            final VirtualFile[] sourceRoots
                    = projectRootManager.getContentSourceRoots();

            // disable if no files are selected
            if (sourceRoots == null || sourceRoots.length == 0) {
                presentation.setEnabled(false);

            } else {
                presentation.setEnabled(!checkStylePlugin.isScanInProgress());
            }
        } catch (Throwable e) {
            final CheckStylePluginException processed
                    = CheckStylePlugin.processError(null, e);
            if (processed != null) {
                LOG.error("Button update failed", processed);
            }
        }
    }
}
