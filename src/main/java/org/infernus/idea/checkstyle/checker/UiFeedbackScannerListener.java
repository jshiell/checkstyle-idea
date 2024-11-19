package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.model.ScanResult;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class UiFeedbackScannerListener implements ScannerListener {
    private final Project project;

    public UiFeedbackScannerListener(final Project project) {
        this.project = project;
    }


    @Override
    public void scanStarting(final List<PsiFile> filesToScan) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final CheckStyleToolWindowPanel toolWindowPanel = toolWindowPanel();
            if (toolWindowPanel != null) {
                toolWindowPanel.displayInProgress(filesToScan.size());
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
    public void scanCompletedSuccessfully(final ScanResult scanResult) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final CheckStyleToolWindowPanel toolWindowPanel = toolWindowPanel();
            if (toolWindowPanel != null) {
                switch (scanResult.configurationLocationResult().status()) {
                    case NOT_PRESENT:
                        toolWindowPanel.displayWarningResult("plugin.results.no-rules-file");
                        break;
                    case BLOCKED:
                        toolWindowPanel.displayWarningResult("plugin.results.rules-blocked",
                                scanResult.configurationLocationResult().location().blockedForSeconds());
                        break;
                    default:
                        toolWindowPanel.displayResults(scanResult);
                }
            }
        });
    }

    @Override
    public void scanFailedWithError(final CheckStylePluginException error) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final CheckStyleToolWindowPanel toolWindowPanel = toolWindowPanel();
            if (toolWindowPanel != null) {
                toolWindowPanel.displayErrorResult(error);
            }
        });
    }

    @Nullable
    private CheckStyleToolWindowPanel toolWindowPanel() {
        return CheckStyleToolWindowPanel.panelFor(project);
    }
}
