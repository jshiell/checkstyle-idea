package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;

import java.util.List;

/**
 * Scan modified files.
 * <p/>
 * If the project is not setup to use VCS then no files will be scanned.
 */
public class ScanModifiedFiles extends BaseAction {

    private static final Logger LOG = Logger.getInstance(ScanModifiedFiles.class);

    @Override
    public final void actionPerformed(final AnActionEvent event) {
        Project project;
        try {
            project = DataKeys.PROJECT.getData(event.getDataContext());
            if (project == null) {
                return;
            }

            final ToolWindow toolWindow = ToolWindowManager.getInstance(project)
                    .getToolWindow(CheckStyleToolWindowPanel.ID_TOOLWINDOW);

            final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
            project.getComponent(CheckStylePlugin.class).asyncScanFiles(changeListManager.getAffectedFiles(),
                    getSelectedOverride(toolWindow));
        } catch (Throwable e) {
            LOG.warn("Modified files scan failed", e);
        }
    }

    @Override
    public void update(final AnActionEvent event) {
        super.update(event);

        Project project = null;
        try {
            project = DataKeys.PROJECT.getData(event.getDataContext());
            if (project == null) { // check if we're loading...
                return;
            }

            final CheckStylePlugin checkStylePlugin = project.getComponent(CheckStylePlugin.class);
            if (checkStylePlugin == null) {
                throw new IllegalStateException("Couldn't get checkstyle plugin");
            }

            final Presentation presentation = event.getPresentation();

            // disable if no files are modified
            final List<VirtualFile> modifiedFiles = ChangeListManager.getInstance(project).getAffectedFiles();
            if (modifiedFiles.isEmpty()) {
                presentation.setEnabled(false);
            } else {
                presentation.setEnabled(!checkStylePlugin.isScanInProgress());
            }
        } catch (Throwable e) {
            LOG.warn("Button update failed.", e);
        }
    }
}
