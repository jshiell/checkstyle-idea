package org.infernus.idea.checkstyle.actions;

import java.util.Arrays;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;
import org.infernus.idea.checkstyle.util.FileTypes;

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
            final ScanScope scope = checkStylePlugin.getConfiguration().getScanScope();

            final ToolWindow toolWindow = ToolWindowManager.getInstance(
                    project).getToolWindow(CheckStyleToolWindowPanel.ID_TOOLWINDOW);
            toolWindow.activate(() -> {
                try {
                    setProgressText(toolWindow, "plugin.status.in-progress.current");

                    final VirtualFile selectedFile = getSelectedFile(project, scope);
                    if (selectedFile != null) {
                        project.getComponent(CheckStylePlugin.class).asyncScanFiles(
                                Arrays.asList(selectedFile), getSelectedOverride(toolWindow));
                    }

                } catch (Throwable e) {
                    CheckStylePlugin.processErrorAndLog("Current File scan", e);
                }
            });

        } catch (Throwable e) {
            CheckStylePlugin.processErrorAndLog("Current File scan", e);
        }
    }

    private VirtualFile getSelectedFile(final Project project, final ScanScope scope) {

        VirtualFile selectedFile = null;

        final Editor selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (selectedTextEditor != null) {
            selectedFile = FileDocumentManager.getInstance().getFile(selectedTextEditor.getDocument());
        }

        if (selectedFile == null) {
            // this is the preferred solution, but it doesn't respect the focus of split editors at present
            final VirtualFile[] selectedFiles = FileEditorManager.getInstance(project).getSelectedFiles();
            if (selectedFiles.length > 0) {
                selectedFile = selectedFiles[0];
            }
        }

        // validate selected file against scan scope
        if (selectedFile != null && scope != ScanScope.Everything) {
            final ProjectFileIndex projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project);
            if (projectFileIndex != null) {
                if (!projectFileIndex.isInSourceContent(selectedFile)) {
                    selectedFile = null;
                }
                if (!scope.includeNonJavaSources() && selectedFile != null) {
                    if (!FileTypes.isJava(selectedFile.getFileType())) {
                        selectedFile = null;
                    }
                }
                if (!scope.includeTestClasses() && selectedFile != null) {
                    if (projectFileIndex.isInTestSourceContent(selectedFile)) {
                        selectedFile = null;
                    }
                }
            }
        }
        return selectedFile;
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
            final ScanScope scope = checkStylePlugin.getConfiguration().getScanScope();

            final VirtualFile selectedFile = getSelectedFile(project, scope);

            // disable if no file is selected or scan in progress
            final Presentation presentation = event.getPresentation();
            if (selectedFile != null) {
                presentation.setEnabled(!checkStylePlugin.isScanInProgress());
            } else {
                presentation.setEnabled(false);
            }
        } catch (Throwable e) {
            CheckStylePlugin.processErrorAndLog("Current File button update", e);
        }
    }
}
