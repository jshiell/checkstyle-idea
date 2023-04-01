package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.concurrency.NonUrgentExecutor;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.jetbrains.annotations.NotNull;

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

                        ThrowableRunnable<RuntimeException> scanAction = null;
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
                            ReadAction.run(scanAction);
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
        final Presentation presentation = event.getPresentation();

        project(event).ifPresentOrElse(project -> ReadAction.nonBlocking(() -> {
                    try {
                        final VirtualFile[] selectedFiles = FileEditorManager.getInstance(project).getSelectedFiles();
                        if (selectedFiles.length == 0) {
                            return VirtualFile.EMPTY_ARRAY;
                        }

                        final Module module = ModuleUtil.findModuleForFile(selectedFiles[0], project);
                        if (module == null) {
                            return VirtualFile.EMPTY_ARRAY;
                        }

                        final ScanScope scope = configurationManager(project).getCurrent().getScanScope();

                        final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
                        if (scope == ScanScope.Everything) {
                            return moduleRootManager.getContentRoots();
                        } else {
                            return moduleRootManager.getSourceRoots(scope.includeTestClasses());
                        }
                    } catch (Throwable e) {
                        LOG.warn("Current Module button update failed", e);
                        return VirtualFile.EMPTY_ARRAY;
                    }

                }).finishOnUiThread(ModalityState.any(), (moduleFiles) -> {
                    // disable if no files are selected or scan in progress
                    if (containsAtLeastOneFile(moduleFiles)) {
                        presentation.setEnabled(!staticScanner(project).isScanInProgress());
                    } else {
                        presentation.setEnabled(false);
                    }
                }).submit(NonUrgentExecutor.getInstance()),
                () -> presentation.setEnabled(false));
    }
}
