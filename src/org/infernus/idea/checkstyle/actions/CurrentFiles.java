package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.vfs.VirtualFile;
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
    public void actionPerformed(final AnActionEvent event) {
        final Project project = (Project) event.getDataContext().getData(DataConstants.PROJECT);

        final ToolWindow toolWindow = ToolWindowManager.getInstance(
                project).getToolWindow(CheckStyleConstants.ID_TOOL_WINDOW);
        toolWindow.activate(null);

        final CheckStylePlugin checkStylePlugin = project.getComponent(CheckStylePlugin.class);
        if (checkStylePlugin != null) {
            project.getComponent(CheckStylePlugin.class).checkCurrentFile(event);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final AnActionEvent event) {
        super.update(event);

        final VirtualFile[] selectedFiles = (VirtualFile[]) event.getDataContext().getData(
                DataConstants.VIRTUAL_FILE_ARRAY);

        // disable if no files are selected
        final Presentation presentation = event.getPresentation();
        if (selectedFiles == null || selectedFiles.length == 0) {
            presentation.setEnabled(false);

        } else {
            presentation.setEnabled(true);
        }


    }
}
