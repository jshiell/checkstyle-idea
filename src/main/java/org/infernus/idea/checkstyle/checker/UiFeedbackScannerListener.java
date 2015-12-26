package org.infernus.idea.checkstyle.checker;

import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Map;

public class UiFeedbackScannerListener implements ScannerListener {
    private final CheckStylePlugin plugin;

    public UiFeedbackScannerListener(final CheckStylePlugin plugin) {
        this.plugin = plugin;
    }


    @Override
    public void scanStarting(final List<PsiFile> filesToScan) {
        SwingUtilities.invokeLater(() -> {
            final CheckStyleToolWindowPanel toolWindowPanel = toolWindowPanel();
            if (toolWindowPanel != null) {
                toolWindowPanel.displayInProgress(filesToScan.size());
            }
        });
    }

    @Override
    public void filesScanned(final int count) {
        SwingUtilities.invokeLater(() -> {
            final CheckStyleToolWindowPanel toolWindowPanel = CheckStyleToolWindowPanel.panelFor(plugin.getProject());
            if (toolWindowPanel != null) {
                toolWindowPanel.incrementProgressBarBy(count);
            }
        });
    }

    @Override
    public void scanComplete(final ConfigurationLocationStatus configurationLocationStatus,
                             final Map<PsiFile, List<Problem>> scanResults) {
        SwingUtilities.invokeLater(() -> {
            final CheckStyleToolWindowPanel toolWindowPanel = toolWindowPanel();
            if (toolWindowPanel != null) {
                switch (configurationLocationStatus) {
                    case NOT_PRESENT:
                        toolWindowPanel.displayWarningResult("plugin.results.no-rules-file");
                        break;
                    case BLACKLISTED:
                        toolWindowPanel.displayWarningResult("plugin.results.rules-blacklist");
                        break;
                    default:
                        toolWindowPanel.displayResults(scanResults);
                }
            }
        });
    }

    @Override
    public void errorCaught(final CheckStylePluginException error) {
        SwingUtilities.invokeLater(() -> {
            final CheckStyleToolWindowPanel toolWindowPanel = toolWindowPanel();
            if (toolWindowPanel != null) {
                toolWindowPanel.displayErrorResult(error);
            }
        });
    }

    @Nullable
    private CheckStyleToolWindowPanel toolWindowPanel() {
        return CheckStyleToolWindowPanel.panelFor(plugin.getProject());
    }
}
