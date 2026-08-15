package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.model.ScanResult;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static org.infernus.idea.checkstyle.checker.ResultHandling.MERGE;

public class UiFeedbackScannerListener implements ScannerListener {
    private final Project project;
    private final ResultHandling resultHandling;

    public UiFeedbackScannerListener(final Project project) {
        this(project, ResultHandling.REPLACE);
    }

    public UiFeedbackScannerListener(final Project project,
                                     @NotNull final ResultHandling resultHandling) {
        this.project = project;
        this.resultHandling = resultHandling;
    }


    @Override
    public void scanStarting(final List<PsiFile> filesToScan) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final CheckStyleToolWindowPanel toolWindowPanel = toolWindowPanel();
            if (toolWindowPanel != null) {
                if (resultHandling == MERGE) {
                    toolWindowPanel.displayRefreshInProgress(filesToScan.size());
                } else {
                    toolWindowPanel.displayInProgress(filesToScan.size());
                }
            }
        });
    }

    @Override
    public void filesScanned(final int count) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final CheckStyleToolWindowPanel toolWindowPanel = CheckStyleToolWindowPanel.panelFor(project);
            if (toolWindowPanel != null) {
                toolWindowPanel.incrementProgressBarBy(count);
            }
        });
    }

    @Override
    public void scanCompletedSuccessfully(final List<ScanResult> scanResults) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final CheckStyleToolWindowPanel toolWindowPanel = toolWindowPanel();
            if (toolWindowPanel != null) {
                final ScanOutcome outcome = ScanOutcome.of(scanResults);

                if (resultHandling == MERGE) {
                    if (outcome.hasBlockedRulesFiles()) {
                        // a blocked rules file is dropped before the files are scanned, so merging would
                        // strip that rules file's findings with nothing to put in their place
                        toolWindowPanel.clearProgress();
                        toolWindowPanel.setProgressText(outcome.warningMessage());
                    } else {
                        toolWindowPanel.mergeResults(outcome.validResults(), outcome.warningMessage());
                    }
                } else {
                    toolWindowPanel.displayResults(outcome.validResults(), outcome.warningMessage());
                }
            }
        });
    }

    @Override
    public void scanFailedWithError(final CheckStylePluginException error) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final CheckStyleToolWindowPanel toolWindowPanel = toolWindowPanel();
            if (toolWindowPanel != null) {
                if (resultHandling == MERGE) {
                    // a failure while refreshing must not cost the user every other file's results
                    toolWindowPanel.clearProgress();
                    toolWindowPanel.setProgressText(CheckStyleBundle.message("plugin.results.error"));
                } else {
                    toolWindowPanel.displayErrorResult(error);
                }
            }
        });
    }

    @Nullable
    private CheckStyleToolWindowPanel toolWindowPanel() {
        return CheckStyleToolWindowPanel.panelFor(project);
    }
}
