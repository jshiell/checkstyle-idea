package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;
import org.infernus.idea.checkstyle.util.CheckStyleUtilities;

import java.util.Arrays;

/**
 * Action to execute a CheckStyle scan on the current editor file.
 */
public class ScanCurrentFile extends BaseAction {

    @Override
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
                    project).getToolWindow(CheckStyleToolWindowPanel.ID_TOOLWINDOW);
            toolWindow.activate(new Runnable() {
                @Override
                public void run() {
                    try {
                        setProgressText(toolWindow, "plugin.status.in-progress.current");

                        final VirtualFile selectedFile = getSelectedFile(project);
                        if (selectedFile != null) {
                            project.getComponent(CheckStylePlugin.class).checkFiles(
                                    Arrays.asList(selectedFile), getSelectedOverride(toolWindow));
                        }

                    } catch (Throwable e) {
                        CheckStylePlugin.processErrorAndLog("Current File scan", e);
                    }
                }
            });

        } catch (Throwable e) {
            CheckStylePlugin.processErrorAndLog("Current File scan", e);
        }
    }

    private VirtualFile getSelectedFile(final Project project) {
        final Editor selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (selectedTextEditor != null) {
            final VirtualFile selectedFile = FileDocumentManager.getInstance().getFile(selectedTextEditor.getDocument());
            if (selectedFile != null) {
                return selectedFile;
            }
        }

        // this is the preferred solution, but it doesn't respect the focus of split editors at present
        final VirtualFile[] selectedFiles = FileEditorManager.getInstance(project).getSelectedFiles();
        if (selectedFiles != null && selectedFiles.length > 0) {
            return selectedFiles[0];
        }

        return null;
    }

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

            final boolean scanOnlyJavaFiles = !checkStylePlugin.getConfiguration().isScanningNonJavaFiles();
            final VirtualFile selectedFile = getSelectedFile(project);

            // disable if no files are selected
            final Presentation presentation = event.getPresentation();
            if (selectedFile == null) {
                presentation.setEnabled(false);

            } else if (scanOnlyJavaFiles) {
                // check files are valid
                if (!CheckStyleUtilities.isJavaFile(selectedFile.getFileType())) {
                    presentation.setEnabled(false);
                    return;
                }

                presentation.setEnabled(!checkStylePlugin.isScanInProgress());
            }
        } catch (Throwable e) {
            CheckStylePlugin.processErrorAndLog("Current File button update", e);
        }
    }
}
