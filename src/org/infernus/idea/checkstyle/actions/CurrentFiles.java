package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.infernus.idea.checkstyle.CheckStyleConstants;
import org.infernus.idea.checkstyle.CheckStylePlugin;

/**
 * Action to execute a CheckStyle scan on the currently selected file.
 *
 * @author James Shiell
 * @version 1.0
 */
public class CurrentFiles extends BaseAction {


    /**
     * {@inheritDoc}
     */
    public void actionPerformed(final AnActionEvent event)
    {
        final Project project = (Project) event.getDataContext().getData(DataConstants.PROJECT);

        final CheckStylePlugin checkStylePlugin
                = project.getComponent(CheckStylePlugin.class);
        if (checkStylePlugin == null) {
            throw new IllegalStateException("Couldn't get checkstyle plugin");
        }

        final ToolWindow toolWindow = ToolWindowManager.getInstance(
                project).getToolWindow(checkStylePlugin.getToolWindowId());
        toolWindow.activate(null);

        project.getComponent(CheckStylePlugin.class).checkCurrentFile(event);
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

        // todo this only operates on open file, not selected
        final VirtualFile[] selectedFiles
                = FileEditorManager.getInstance(project).getSelectedFiles();
        
        // disable if no files are selected
        final Presentation presentation = event.getPresentation();
        if (selectedFiles == null || selectedFiles.length == 0) {
            presentation.setEnabled(false);

        } else {
            // check files are valid
            for (final VirtualFile file : selectedFiles) {
                // todo refactor check to utility method
                if (!CheckStyleConstants.FILETYPE_JAVA.equals(file.getFileType())) {
                    presentation.setEnabled(false);
                    return;
                }
            }

            presentation.setEnabled(true);
        }


    }
}
