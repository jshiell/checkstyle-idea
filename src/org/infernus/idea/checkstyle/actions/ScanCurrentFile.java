package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.util.CheckStyleUtilities;

/**
 * Action to execute a CheckStyle scan on the current editor file.
 *
 * @author James Shiell
 * @version 1.0
 */
public class ScanCurrentFile extends BaseAction {

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(final AnActionEvent event) {
        final Project project = (Project) event.getDataContext().getData(
                DataConstants.PROJECT);
        if (project == null) {
            return;
        }

        final CheckStylePlugin checkStylePlugin
                = project.getComponent(CheckStylePlugin.class);
        if (checkStylePlugin == null) {
            throw new IllegalStateException("Couldn't get checkstyle plugin");
        }

        final ToolWindow toolWindow = ToolWindowManager.getInstance(
                project).getToolWindow(checkStylePlugin.getToolWindowId());
        toolWindow.activate(null);

        // read select file
        final VirtualFile[] selectedFiles
                = FileEditorManager.getInstance(project).getSelectedFiles();
        if (selectedFiles != null && selectedFiles.length > 0) {
            project.getComponent(CheckStylePlugin.class).checkFiles(
                    selectedFiles, event);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final AnActionEvent event) {
        super.update(event);

        final Project project = (Project) event.getDataContext().getData(
                DataConstants.PROJECT);
        if (project == null) { // check if we're loading...
            return;
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

            presentation.setEnabled(true);
        }


    }
}
