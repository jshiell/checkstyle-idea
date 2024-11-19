package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ScanResult;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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
    public void scanCompletedSuccessfully(final List<ScanResult> scanResults) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final CheckStyleToolWindowPanel toolWindowPanel = toolWindowPanel();
            if (toolWindowPanel != null) {
                var notPresent = new ArrayList<ConfigurationLocation>();
                var blocked = new ArrayList<ConfigurationLocation>();
                var validResults = new ArrayList<ScanResult>();

                for (ScanResult scanResult : scanResults) {
                    switch (scanResult.configurationLocationResult().status()) {
                        case NOT_PRESENT -> notPresent.add(scanResult.configurationLocationResult().location());
                        case BLOCKED -> blocked.add(scanResult.configurationLocationResult().location());
                        default -> validResults.add(scanResult);
                    }
                }

                var warningMessages = new ArrayList<String>();
                if (!notPresent.isEmpty()) {
                    warningMessages.add(CheckStyleBundle.message("plugin.results.no-rules-file"));
                }
                if (!blocked.isEmpty()) {
                    var maxTimeBlocked = blocked.stream().map(ConfigurationLocation::blockedForSeconds).reduce(Long::max).get();
                    var blockedLocations = String.join(", ", blocked.stream().map(ConfigurationLocation::getDescription).toList());
                    warningMessages.add(CheckStyleBundle.message("plugin.results.rules-blocked", maxTimeBlocked, blockedLocations));
                }

                toolWindowPanel.displayResults(validResults, String.join("; ", warningMessages));
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
