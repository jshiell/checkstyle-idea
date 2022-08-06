package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static org.infernus.idea.checkstyle.actions.ToolWindowAccess.toolWindow;

/**
 * Action to execute a CheckStyle scan on the current module.
 */
public class ScanModule extends BaseAction {
    private static final Logger LOG = Logger.getInstance(ScanModule.class);

    @Override
    public final void actionPerformed(final @NotNull AnActionEvent event) {
        project(event).ifPresent(project -> {
            try {
                final ToolWindow toolWindow = toolWindow(project);

                final VirtualFile[] selectedFiles
                        = FileEditorManager.getInstance(project).getSelectedFiles();
                if (selectedFiles.length == 0) {
                    setProgressText(toolWindow, "plugin.status.in-progress.no-file");
                    return;
                }

                final Module module = ModuleUtil.findModuleForFile(
                        selectedFiles[0], project);
                if (module == null) {
                    setProgressText(toolWindow, "plugin.status.in-progress.no-module");
                    return;
                }

                final ScanScope scope = configurationManager(project).getCurrent().getScanScope();

                toolWindow.activate(() -> {
                    try {
                        setProgressText(toolWindow, "plugin.status.in-progress.module");

                        Runnable scanAction = null;
                        if (scope == ScanScope.Everything) {
                            scanAction = new ScanAllFilesInModuleTask(module, getSelectedOverride(toolWindow));
                        } else {
                            final VirtualFile[] moduleSourceRoots =
                                    ModuleRootManager.getInstance(module).getSourceRoots(scope.includeTestClasses());
                            if (moduleSourceRoots.length > 0) {
                                scanAction = new ScanAllGivenFilesTask(project, moduleSourceRoots,
                                        getSelectedOverride(toolWindow));
                            }
                        }
                        if (scanAction != null) {
                            ApplicationManager.getApplication().runReadAction(scanAction);
                        }
                    } catch (Throwable e) {
                        LOG.warn("Current Module scan failed", e);
                    }
                });

            } catch (Throwable e) {
                LOG.warn("Current Module scan failed", e);
            }
        });
    }

    @Override
    public final void update(final @NotNull AnActionEvent event) {
        super.update(event);

        try {
            final Presentation presentation = event.getPresentation();

            Optional<Project> projectFromEvent = project(event);
            if (projectFromEvent.isEmpty()) { // check if we're loading...
                presentation.setEnabled(false);
                return;
            }

            projectFromEvent.ifPresent(project -> {
                final VirtualFile[] selectedFiles
                        = FileEditorManager.getInstance(project).getSelectedFiles();
                if (selectedFiles.length == 0) {
                    return;
                }

                final Module module = ModuleUtil.findModuleForFile(
                        selectedFiles[0], project);
                if (module == null) {
                    return;
                }

                final ScanScope scope = configurationManager(project).getCurrent().getScanScope();

                VirtualFile[] moduleFiles;
                final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
                if (scope == ScanScope.Everything) {
                    moduleFiles = moduleRootManager.getContentRoots();
                } else {
                    moduleFiles = moduleRootManager.getSourceRoots(scope.includeTestClasses());
                }

                // disable if no files are selected or scan in progress
                if (containsAtLeastOneFile(moduleFiles)) {
                    presentation.setEnabled(!staticScanner(project).isScanInProgress());
                } else {
                    presentation.setEnabled(false);
                }
            });
        } catch (Throwable e) {
            LOG.warn("Current Module button update failed", e);
        }
    }
}
