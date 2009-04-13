package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStyleConstants;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;

import java.util.Arrays;

/**
 * Action to execute a CheckStyle scan on the current module.
 *
 * @author James Shiell
 * @version 1.0
 */
public class ScanModule extends BaseAction {

    /**
     * Logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(
            ScanModule.class);

    /**
     * {@inheritDoc}
     */
    public final void actionPerformed(final AnActionEvent event) {
        try {
            final Project project = DataKeys.PROJECT.getData(event.getDataContext());
            if (project == null) {
                return;
            }

            final ToolWindow toolWindow = ToolWindowManager.getInstance(
                    project).getToolWindow(CheckStyleConstants.ID_TOOLWINDOW);

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

            toolWindow.activate(null);

            setProgressText(toolWindow, "plugin.status.in-progress.module");

            // find module files
            final ModuleRootManager moduleRootManager
                    = ModuleRootManager.getInstance(module);
            final VirtualFile[] moduleFiles = moduleRootManager.getSourceRoots();

            if (moduleFiles.length > 0) {
                project.getComponent(CheckStylePlugin.class).checkFiles(
                        Arrays.asList(moduleFiles), event);
            }

        } catch (Throwable e) {
            final CheckStylePluginException processed
                    = CheckStylePlugin.processError(null, e);
            if (processed != null) {
                LOG.error("Current module scan failed", processed);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void update(final AnActionEvent event) {
        super.update(event);

        try {
            final Presentation presentation = event.getPresentation();

            final Project project = DataKeys.PROJECT.getData(event.getDataContext());
            if (project == null) { // check if we're loading...
                presentation.setEnabled(false);
                return;
            }

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

            ModuleRootManager moduleRootManager
                    = ModuleRootManager.getInstance(module);
            final VirtualFile[] moduleFiles = moduleRootManager.getSourceRoots();

            // disable if no files are selected
            if (moduleFiles.length == 0) {
                presentation.setEnabled(false);

            } else {
                presentation.setEnabled(!checkStylePlugin.isScanInProgress());
            }
        } catch (Throwable e) {
            final CheckStylePluginException processed
                    = CheckStylePlugin.processError(null, e);
            if (processed != null) {
                LOG.error("Button update failed", processed);
            }
        }
    }
}
