package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import org.jetbrains.annotations.NotNull;

import static org.infernus.idea.checkstyle.actions.ToolWindowAccess.toolWindow;

/**
 * Scan modified files.
 * <p/>
 * If the project is not setup to use VCS then no files will be scanned.
 */
public class ScanModifiedFiles extends BaseAction {

    private static final Logger LOG = Logger.getInstance(ScanModifiedFiles.class);

    @Override
    public final void actionPerformed(final @NotNull AnActionEvent event) {
        project(event).ifPresent(project -> {
            try {
                final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
                staticScanner(project).asyncScanFiles(
                        changeListManager.getAffectedFiles(),
                        getSelectedOverride(toolWindow(project)));
            } catch (Throwable e) {
                LOG.warn("Modified files scan failed", e);
            }
        });
    }

    @Override
    public void update(final @NotNull AnActionEvent event) {
        final Presentation presentation = event.getPresentation();

        project(event).ifPresentOrElse(project -> {
            try {
                if (ChangeListManager.getInstance(project).getAffectedFiles().isEmpty()) {
                    presentation.setEnabled(false);
                } else {
                    presentation.setEnabled(!staticScanner(project).isScanInProgress());
                }

            } catch (Throwable e) {
                LOG.warn("Button update failed.", e);
            }
        }, () -> presentation.setEnabled(false));
    }
}
