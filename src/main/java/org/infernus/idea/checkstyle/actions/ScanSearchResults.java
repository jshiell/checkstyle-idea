package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageView;
import com.intellij.usages.rules.UsageInFile;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static org.infernus.idea.checkstyle.actions.ToolWindowAccess.toolWindow;

/**
 * Base class for actions that scan the files behind a set of search results.
 */
public abstract class ScanSearchResults extends BaseAction {

    private static final Logger LOG = Logger.getInstance(ScanSearchResults.class);

    /**
     * The files behind the search results this action applies to. Called on the EDT.
     */
    protected abstract List<VirtualFile> filesToScan(@NotNull AnActionEvent event);

    @Override
    public final void actionPerformed(final @NotNull AnActionEvent event) {
        project(event).ifPresent(project -> {
            try {
                final ToolWindow toolWindow = toolWindow(project);
                final List<VirtualFile> files = filesToScan(event);

                toolWindow.activate(() -> {
                    if (files.isEmpty()) {
                        final CheckStyleToolWindowPanel panel = CheckStyleToolWindowPanel.panelFor(project);
                        if (panel != null) {
                            panel.displayWarningResult("plugin.status.in-progress.no-search-results");
                        }
                    } else {
                        setProgressText(toolWindow, "plugin.status.in-progress.search-results");
                        staticScanner(project).asyncScanFiles(files, getSelectedOverride(toolWindow));
                    }
                });

            } catch (Throwable e) {
                LOG.warn("Search results scan failed", e);
            }
        });
    }

    @Override
    public void update(final @NotNull AnActionEvent event) {
        final Presentation presentation = event.getPresentation();
        final Project project = getEventProject(event);

        // this action is registered in an IDE-wide popup, so it must hide itself outside of usage views
        final boolean inUsageView = project != null && event.getData(UsageView.USAGE_VIEW_KEY) != null;

        presentation.setVisible(inUsageView);
        presentation.setEnabled(inUsageView && !staticScanner(project).isScanInProgress());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    static List<VirtualFile> filesFrom(@NotNull final Collection<Usage> usages) {
        return usages.stream()
                .filter(Usage::isValid)
                .filter(UsageInFile.class::isInstance)
                .map(usage -> ((UsageInFile) usage).getFile())
                .filter(Objects::nonNull)
                .filter(VirtualFile::isValid)
                .filter(file -> !file.isDirectory())
                .distinct()
                .toList();
    }
}
