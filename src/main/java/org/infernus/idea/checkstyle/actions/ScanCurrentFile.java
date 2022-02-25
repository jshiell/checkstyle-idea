package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import org.infernus.idea.checkstyle.config.PluginConfiguration;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.infernus.idea.checkstyle.util.FileTypes;

import static java.util.Collections.singletonList;
import static org.infernus.idea.checkstyle.actions.ToolWindowAccess.toolWindow;

/**
 * Action to execute a CheckStyle scan on the current editor file.
 */
public class ScanCurrentFile extends BaseAction {
    private static final Logger LOG = Logger.getInstance(ScanCurrentFile.class);

    @Override
    public void actionPerformed(final AnActionEvent event) {
        project(event).ifPresent(project -> {
            try {
                final PluginConfiguration pluginConfiguration = configurationManager(project).getCurrent();

                final ToolWindow toolWindow = toolWindow(project);
                toolWindow.activate(() -> {
                    try {
                        setProgressText(toolWindow, "plugin.status.in-progress.current");

                        final VirtualFile selectedFile = getSelectedFile(project, pluginConfiguration);
                        if (selectedFile != null) {
                            staticScanner(project).asyncScanFiles(
                                    singletonList(selectedFile), getSelectedOverride(toolWindow));
                        }

                    } catch (Throwable e) {
                        LOG.warn("Current File scan failed", e);
                    }
                });

            } catch (Throwable e) {
                LOG.warn("Current File scan failed", e);
            }
        });
    }


    private VirtualFile getSelectedFile(final Project project, PluginConfiguration pluginConfiguration) {
        VirtualFile selectedFile = null;
        final ScanScope scanScope = pluginConfiguration.getScanScope();

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
        if (selectedFile != null && scanScope != ScanScope.Everything) {
            final ProjectFileIndex projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project);
            if (projectFileIndex != null) {
                if (!projectFileIndex.isInSourceContent(selectedFile)) {
                    selectedFile = null;
                }
                if (!scanScope.includeNonJavaSources() && selectedFile != null) {
                    if (!FileTypes.isJava(selectedFile.getFileType())) {
                        selectedFile = null;
                    }
                }
                if (!scanScope.includeTestClasses() && selectedFile != null) {
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

        project(event).ifPresent(project -> {
            try {
                final PluginConfiguration pluginConfiguration = configurationManager(project).getCurrent();

                final VirtualFile selectedFile = getSelectedFile(project, pluginConfiguration);

                // disable if no file is selected or scan in progress
                final Presentation presentation = event.getPresentation();
                if (selectedFile != null) {
                    presentation.setEnabled(!staticScanner(project).isScanInProgress());
                } else {
                    presentation.setEnabled(false);
                }
            } catch (Throwable e) {
                LOG.warn("Current File button update failed", e);
            }
        });
    }
}
