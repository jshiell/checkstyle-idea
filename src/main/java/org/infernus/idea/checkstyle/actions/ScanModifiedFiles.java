package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.util.concurrency.NonUrgentExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Optional;

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
        super.update(event);

        final Presentation presentation = event.getPresentation();
        final Optional<Project> projectFromEvent = project(event);
        if (projectFromEvent.isEmpty()) { // check if we're loading...
            presentation.setEnabled(false);
            return;
        }

        projectFromEvent.ifPresent(project -> ReadAction.nonBlocking(() -> {
            try {
                return ChangeListManager.getInstance(project).getAffectedFiles();

            } catch (Throwable e) {
                LOG.warn("Button update failed.", e);
                return Collections.EMPTY_LIST;
            }
        }).finishOnUiThread(ModalityState.any(), (modifiedFiles) -> {
            // disable if no files are modified
            if (modifiedFiles.isEmpty()) {
                presentation.setEnabled(false);
            } else {
                presentation.setEnabled(!staticScanner(project).isScanInProgress());
            }
        }).submit(NonUrgentExecutor.getInstance()));
    }
}
