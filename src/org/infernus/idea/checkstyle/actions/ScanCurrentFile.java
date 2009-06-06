package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStyleConstants;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.util.CheckStyleUtilities;

import java.util.Arrays;

/**
 * Action to execute a CheckStyle scan on the current editor file.
 *
 * @author James Shiell
 * @version 1.0
 */
public class ScanCurrentFile extends BaseAction {

    /**
     * Logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(
            ScanCurrentFile.class);

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(final AnActionEvent event) {
        final Project project = DataKeys.PROJECT.getData(event.getDataContext());
        if (project == null) {
            return;
        }

        try {
            final CheckStylePlugin checkStylePlugin
                    = project.getComponent(CheckStylePlugin.class);
            if (checkStylePlugin == null) {
                throw new IllegalStateException("Couldn't get checkstyle plugin");
            }

            final ToolWindow toolWindow = ToolWindowManager.getInstance(
                    project).getToolWindow(CheckStyleConstants.ID_TOOLWINDOW);
            toolWindow.activate(null);

            setProgressText(toolWindow, "plugin.status.in-progress.current");

            // read select file
            final VirtualFile[] selectedFiles
                    = FileEditorManager.getInstance(project).getSelectedFiles();
            if (selectedFiles.length > 0) {
                project.getComponent(CheckStylePlugin.class).checkFiles(
                        Arrays.asList(selectedFiles));
            }
        } catch (Throwable e) {
            final CheckStylePluginException processed
                    = CheckStylePlugin.processError(null, e);
            if (processed != null) {
                LOG.error("Current file scan failed", processed);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final AnActionEvent event) {
        super.update(event);

        try {
            final Project project = DataKeys.PROJECT.getData(event.getDataContext());
            if (project == null) { // check if we're loading...
                return;
            }

            final CheckStylePlugin checkStylePlugin
                    = project.getComponent(CheckStylePlugin.class);
            if (checkStylePlugin == null) {
                throw new IllegalStateException("Couldn't get checkstyle plugin");
            }

            final VirtualFile[] selectedFiles
                    = FileEditorManager.getInstance(project).getSelectedFiles();

            // disable if no files are selected
            final Presentation presentation = event.getPresentation();
            if (selectedFiles == null || selectedFiles.length == 0) {
                presentation.setEnabled(false);

            } else {
                // check files are valid
                for (final VirtualFile file : selectedFiles) {
                    if (!CheckStyleUtilities.isValidFileType(file.getFileType())) {
                        presentation.setEnabled(false);
                        return;
                    }
                }

                presentation.setEnabled(!checkStylePlugin.isScanInProgress());
            }
        } catch (Throwable e) {
            final CheckStylePluginException processed
                    = CheckStylePlugin.processError(null, e);
            if (processed != null) {
                LOG.error("Button update failed.", processed);
            }
        }
    }
}
