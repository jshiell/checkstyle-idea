package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;

import java.util.Optional;

/**
 * Action to execute a CheckStyle scan on the current module.
 */
public class ScanModule extends BaseAction {

    @Override
    public final void actionPerformed(final AnActionEvent event) {
        project(event).ifPresent(project -> {
            try {
                final ToolWindow toolWindow = ToolWindowManager.getInstance(
                        project).getToolWindow(CheckStyleToolWindowPanel.ID_TOOLWINDOW);

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

                final CheckStylePlugin checkStylePlugin
                        = project.getComponent(CheckStylePlugin.class);
                if (checkStylePlugin == null) {
                    throw new IllegalStateException("Couldn't get checkstyle plugin");
                }
                final ScanScope scope = checkStylePlugin.configurationManager().getCurrent().getScanScope();

                toolWindow.activate(() -> {
                    try {
                        setProgressText(toolWindow, "plugin.status.in-progress.module");

                        Runnable scanAction = null;
                        if (scope == ScanScope.Everything) {
                            scanAction = new ScanEverythingAction(module, getSelectedOverride(toolWindow));
                        } else {
                            final VirtualFile[] moduleSourceRoots =
                                    ModuleRootManager.getInstance(module).getSourceRoots(scope.includeTestClasses());
                            if (moduleSourceRoots.length > 0) {
                                scanAction = new ScanSourceRootsAction(project, moduleSourceRoots,
                                        getSelectedOverride(toolWindow));
                            }
                        }
                        if (scanAction != null) {
                            ApplicationManager.getApplication().runReadAction(scanAction);
                        }
                    } catch (Throwable e) {
                        CheckStylePlugin.processErrorAndLog("Current Module scan", e);
                    }
                });

            } catch (Throwable e) {
                CheckStylePlugin.processErrorAndLog("Current Module scan", e);
            }
        });
    }

    @Override
    public final void update(final AnActionEvent event) {
        super.update(event);

        try {
            final Presentation presentation = event.getPresentation();

            Optional<Project> projectFromEvent = project(event);
            if (!projectFromEvent.isPresent()) { // check if we're loading...
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

                final CheckStylePlugin checkStylePlugin
                        = project.getComponent(CheckStylePlugin.class);
                if (checkStylePlugin == null) {
                    throw new IllegalStateException("Couldn't get checkstyle plugin");
                }
                final ScanScope scope = checkStylePlugin.configurationManager().getCurrent().getScanScope();

                VirtualFile[] moduleFiles;
                final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
                if (scope == ScanScope.Everything) {
                    moduleFiles = moduleRootManager.getContentRoots();
                } else {
                    moduleFiles = moduleRootManager.getSourceRoots(scope.includeTestClasses());
                }

                // disable if no files are selected or scan in progress
                if (containsAtLeastOneFile(moduleFiles)) {
                    presentation.setEnabled(!checkStylePlugin.isScanInProgress());
                } else {
                    presentation.setEnabled(false);
                }
            });
        } catch (Throwable e) {
            CheckStylePlugin.processErrorAndLog("Current Module button update", e);
        }
    }
}
