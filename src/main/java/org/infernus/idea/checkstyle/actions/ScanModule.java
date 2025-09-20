package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;
import org.infernus.idea.checkstyle.util.Async;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;

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
                CheckStyleToolWindowPanel checkStyleToolWindowPanel = CheckStyleToolWindowPanel.panelFor(project);

                final VirtualFile[] selectedFiles
                        = FileEditorManager.getInstance(project).getSelectedFiles();
                if (selectedFiles.length == 0) {
                    setProgressText(toolWindow, "plugin.status.in-progress.no-file");
                    return;
                }

                final Module module = ModuleUtil.findModuleForFile(
                        selectedFiles[0], project);
                if (module == null) {
                    if (checkStyleToolWindowPanel != null) {
                        checkStyleToolWindowPanel.displayWarningResult("plugin.status.in-progress.no-module");
                    }
                    return;
                }

                final ScanScope scope = configurationManager(project).getCurrent().getScanScope();

                toolWindow.activate(() -> {
                    try {
                        final Callable<Void> scanAction;
                        if (scope == ScanScope.Everything) {
                            scanAction = new ScanAllFilesInModuleTask(module, getSelectedOverride(toolWindow));
                        } else {
                            final VirtualFile[] moduleSourceRoots =
                                    ModuleRootManager.getInstance(module).getSourceRoots(scope.includeTestClasses());
                            if (moduleSourceRoots.length > 0) {
                                scanAction = new ScanAllGivenFilesTask(project, moduleSourceRoots,
                                        getSelectedOverride(toolWindow));
                            } else if (checkStyleToolWindowPanel != null) {
                                checkStyleToolWindowPanel.displayWarningResult("plugin.status.in-progress.no-module-source-roots");
                                scanAction = null;
                            } else {
                                scanAction = null;
                            }
                        }
                        if (scanAction != null) {
                            setProgressText(toolWindow, "plugin.status.in-progress.module");
                            Async.executeOnPooledThread(scanAction);
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

        project(event).ifPresentOrElse(
                project -> presentation.setEnabled(!staticScanner(project).isScanInProgress()),
                () -> presentation.setEnabled(false));
    }
}
