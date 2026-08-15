package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.checker.ResultHandling;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;
import org.infernus.idea.checkstyle.util.Async;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.toCollection;
import static org.infernus.idea.checkstyle.actions.ToolWindowAccess.actOnToolWindowPanel;
import static org.infernus.idea.checkstyle.actions.ToolWindowAccess.getFromToolWindowPanel;
import static org.infernus.idea.checkstyle.actions.ToolWindowAccess.toolWindow;

/**
 * Action to re-scan the files behind the current results, merging the outcome into those results.
 */
public class RefreshResults extends BaseAction {

    private static final Logger LOG = Logger.getInstance(RefreshResults.class);

    @Override
    public void actionPerformed(final @NotNull AnActionEvent event) {
        project(event).ifPresent(project -> {
            try {
                final ToolWindow toolWindow = toolWindow(project);
                toolWindow.activate(() -> {
                    try {
                        // resolving on the event thread keeps the retained results single-threaded,
                        // and gives us the read access that reading a virtual file requires
                        final Set<PsiFile> targetFiles = filesToRefresh(toolWindow);
                        final List<VirtualFile> filesToScan = scannableFilesIn(targetFiles);

                        discardResultsForDeletedFiles(toolWindow, targetFiles, filesToScan);

                        if (filesToScan.isEmpty()) {
                            setProgressText(toolWindow, "plugin.status.in-progress.nothing-to-refresh");
                            return;
                        }

                        setProgressText(toolWindow, "plugin.status.in-progress.refresh");
                        rememberSelection(toolWindow, targetFiles);

                        Async.executeOnPooledThread(() -> {
                            staticScanner(project).asyncScanFiles(
                                    filesToScan,
                                    getSelectedOverride(toolWindow),
                                    ResultHandling.MERGE);
                            return null;
                        });

                    } catch (Throwable e) {
                        LOG.warn("Refresh of results failed", e);
                    }
                });

            } catch (Throwable e) {
                LOG.warn("Refresh of results failed", e);
            }
        });
    }

    /**
     * The files the refresh applies to: those selected in the results tree, or every file with
     * results when the selection is empty.
     */
    @NotNull
    private Set<PsiFile> filesToRefresh(final ToolWindow toolWindow) {
        final Set<PsiFile> files = getFromToolWindowPanel(toolWindow, panel -> {
            final Set<PsiFile> selected = panel.selectedFiles();
            return selected.isEmpty() ? panel.allDisplayedFiles() : selected;
        });
        return files != null ? files : Set.of();
    }

    @NotNull
    private List<VirtualFile> scannableFilesIn(final Set<PsiFile> files) {
        return files.stream()
                .map(PsiFile::getVirtualFile)
                .filter(Objects::nonNull)
                .filter(VirtualFile::isValid)
                .filter(virtualFile -> !virtualFile.isDirectory())
                .distinct()
                .toList();
    }

    /**
     * A file deleted since the scan is never sent to the re-scan, so it would never be stripped by
     * the merge and its results would linger in the tree forever.
     */
    private void discardResultsForDeletedFiles(final ToolWindow toolWindow,
                                               final Set<PsiFile> targetFiles,
                                               final List<VirtualFile> filesToScan) {
        final Set<PsiFile> deletedFiles = targetFiles.stream()
                .filter(psiFile -> !filesToScan.contains(psiFile.getVirtualFile()))
                .collect(toCollection(LinkedHashSet::new));
        if (!deletedFiles.isEmpty()) {
            actOnToolWindowPanel(toolWindow, panel -> panel.discardResultsFor(deletedFiles));
        }
    }

    private void rememberSelection(final ToolWindow toolWindow, final Set<PsiFile> targetFiles) {
        actOnToolWindowPanel(toolWindow, panel -> panel.reselectAfterRefresh(targetFiles));
    }

    @Override
    public void update(final @NotNull AnActionEvent event) {
        super.update(event);

        final Presentation presentation = event.getPresentation();
        project(event).ifPresentOrElse(project -> {
            try {
                final ToolWindow toolWindow = toolWindow(project);
                if (toolWindow == null) {
                    presentation.setEnabled(false);
                    return;
                }

                // a tree walk here would be O(problems), and the toolbar polls this often
                final Boolean hasResults = getFromToolWindowPanel(toolWindow, CheckStyleToolWindowPanel::hasResults);
                presentation.setEnabled(Boolean.TRUE.equals(hasResults)
                        && !staticScanner(project).isScanInProgress());

            } catch (Throwable e) {
                LOG.warn("Refresh button update failed", e);
            }
        }, () -> presentation.setEnabled(false));
    }
}
