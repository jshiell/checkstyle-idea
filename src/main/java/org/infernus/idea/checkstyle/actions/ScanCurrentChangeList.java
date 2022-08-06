package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static org.infernus.idea.checkstyle.actions.ToolWindowAccess.toolWindow;

/**
 * Scan files in the current change-list.
 */
public class ScanCurrentChangeList extends BaseAction {

    private static final Logger LOG = Logger.getInstance(ScanCurrentChangeList.class);

    @Override
    public final void actionPerformed(final @NotNull AnActionEvent event) {
        project(event).ifPresent(project -> {
            try {
                final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
                staticScanner(project).asyncScanFiles(filesFor(changeListManager.getDefaultChangeList()), getSelectedOverride(toolWindow(project)));
            } catch (Throwable e) {
                LOG.warn("Modified files scan failed", e);
            }
        });
    }

    private List<VirtualFile> filesFor(final LocalChangeList changeList) {
        if (changeList == null || changeList.getChanges() == null) {
            return Collections.emptyList();
        }

        final Collection<VirtualFile> filesInChanges = new HashSet<>();
        for (Change change : changeList.getChanges()) {
            if (change.getVirtualFile() != null) {
                filesInChanges.add(change.getVirtualFile());
            }
        }

        return new ArrayList<>(filesInChanges);
    }

    @Override
    public void update(final @NotNull AnActionEvent event) {
        super.update(event);
        project(event).ifPresent(project -> {
            try {
                final Presentation presentation = event.getPresentation();

                final LocalChangeList changeList = ChangeListManager.getInstance(project).getDefaultChangeList();
                if (changeList.getChanges() == null || changeList.getChanges().size() == 0) {
                    presentation.setEnabled(false);
                } else {
                    presentation.setEnabled(!staticScanner(project).isScanInProgress());
                }
            } catch (Throwable e) {
                LOG.warn("Button update failed.", e);
            }
        });
    }
}
