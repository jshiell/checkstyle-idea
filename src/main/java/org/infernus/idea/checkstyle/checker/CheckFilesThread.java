package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashMap;
import java.util.List;

public class CheckFilesThread extends AbstractCheckerThread {
    private static final Log LOG = LogFactory.getLog(CheckFilesThread.class);

    public CheckFilesThread(@NotNull final CheckStylePlugin checkStylePlugin,
                            @NotNull final List<VirtualFile> virtualFiles,
                            @Nullable final ConfigurationLocation overrideConfigLocation) {
        super(checkStylePlugin, virtualFiles, overrideConfigLocation);
        this.setFileResults(new HashMap<>());
    }

    @Override
    public void runFileScanner(@NotNull final FileScanner fileScanner) {
        ApplicationManager.getApplication().runReadAction(fileScanner);
    }

    @Override
    public void run() {
        setRunning(true);

        try {
            beginProgressDisplay();
            processFilesForModuleInfoAndScan();
            displayResults();

        } catch (final Throwable e) {
            final CheckStylePluginException processedError = CheckStylePlugin.processError(
                    "An error occurred during a file scan.", e);

            if (processedError != null) {
                LOG.error("An error occurred while scanning a file.", processedError);
                displayErrorResult(processedError);
            }
        }
    }

    private void displayErrorResult(final CheckStylePluginException processedError) {
        SwingUtilities.invokeLater(() -> {
            final CheckStyleToolWindowPanel toolWindowPanel = toolWindowPanel();
            if (toolWindowPanel != null) {
                toolWindowPanel.displayErrorResult(processedError);
            }
            markThreadComplete();
        });
    }

    private void beginProgressDisplay() {
        SwingUtilities.invokeLater(() -> {
            final CheckStyleToolWindowPanel toolWindowPanel = toolWindowPanel();
            if (toolWindowPanel != null) {
                toolWindowPanel.displayInProgress(getFiles().size());
            }
        });
    }

    private void displayResults() {
        SwingUtilities.invokeLater(() -> {
            final CheckStyleToolWindowPanel toolWindowPanel = toolWindowPanel();
            if (toolWindowPanel != null) {
                switch (getConfigurationLocationStatus()) {
                    case NOT_PRESENT:
                        toolWindowPanel.displayWarningResult("plugin.results.no-rules-file");
                        break;
                    case BLACKLISTED:
                        toolWindowPanel.displayWarningResult("plugin.results.rules-blacklist");
                        break;
                    default:
                        toolWindowPanel.displayResults(getFileResults());
                }
            }
            markThreadComplete();
        });
    }

}
