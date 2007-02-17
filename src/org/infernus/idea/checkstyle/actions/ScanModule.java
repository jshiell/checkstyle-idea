package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.CheckStyleConstants;
import org.infernus.idea.checkstyle.toolwindow.ToolWindowPanel;

import java.util.ResourceBundle;

/**
 * Action to execute a CheckStyle scan on the current module.
 *
 * @author James Shiell
 * @version 1.0
 */
public class ScanModule extends BaseAction {


    /**
     * {@inheritDoc}
     */
    public void actionPerformed(final AnActionEvent event) {
        final Project project = (Project) event.getDataContext().getData(
                DataConstants.PROJECT);
        if (project == null) {
            return;
        }

        final Module module = (Module) event.getDataContext().getData(
                DataConstants.MODULE);
        if (module == null) {
            return;
        }

        final CheckStylePlugin checkStylePlugin
                = project.getComponent(CheckStylePlugin.class);
        if (checkStylePlugin == null) {
            throw new IllegalStateException("Couldn't get checkstyle plugin");
        }

        final ToolWindow toolWindow = ToolWindowManager.getInstance(
                project).getToolWindow(checkStylePlugin.getToolWindowId());
        toolWindow.activate(null);

        // show progress text
        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);
        final String progressText = resources.getString(
                "plugin.status.in-progress.module");
        ((ToolWindowPanel) toolWindow.getComponent()).setProgressText(
                progressText);

        // find module files
        ModuleRootManager moduleRootManager
                = ModuleRootManager.getInstance(module);
        final VirtualFile[] virtualFiles = moduleRootManager.getFiles(
                OrderRootType.SOURCES);

        if (virtualFiles != null && virtualFiles.length > 0) {
            project.getComponent(CheckStylePlugin.class).checkFiles(
                    virtualFiles, event);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final AnActionEvent event) {
        super.update(event);

        final Presentation presentation = event.getPresentation();

        final Project project = (Project) event.getDataContext().getData(
                DataConstants.PROJECT);
        if (project == null) { // check if we're loading...
            presentation.setEnabled(false);
            return;
        }

        final Module module = (Module) event.getDataContext().getData(
                DataConstants.MODULE);
        presentation.setEnabled(false);
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
        final VirtualFile[] virtualFiles = moduleRootManager.getFiles(
                OrderRootType.SOURCES);

        // disable if no files are selected
        if (virtualFiles == null || virtualFiles.length == 0) {
            presentation.setEnabled(false);

        } else {
            presentation.setEnabled(!checkStylePlugin.isScanInProgress());
        }


    }
}
