package org.infernus.idea.checkstyle.checker;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;
import org.infernus.idea.checkstyle.util.ModuleClassPathBuilder;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;

public class CheckFilesThread extends AbstractCheckerThread {
    private static final Log LOG = LogFactory.getLog(CheckFilesThread.class);

    /**
     * Create a thread to check the given files.
     *
     * @param checkStylePlugin       CheckStylePlugin.
     * @param moduleClassPathBuilder the class path builder.
     * @param virtualFiles           the files to check.
     * @param overrideConfigLocation if non-null this configuration will be used in preference to the normal configuration.
     */
    public CheckFilesThread(final CheckStylePlugin checkStylePlugin,
                            final ModuleClassPathBuilder moduleClassPathBuilder,
                            final List<VirtualFile> virtualFiles,
                            final ConfigurationLocation overrideConfigLocation) {
        super(checkStylePlugin, moduleClassPathBuilder, virtualFiles, overrideConfigLocation);
        this.setFileResults(new HashMap<PsiFile, List<ProblemDescriptor>>());
    }

    @Override
    public void runFileScanner(final FileScanner fileScanner) throws InterruptedException, InvocationTargetException {
        ApplicationManager.getApplication().runReadAction(fileScanner);
    }

    /**
     * Execute the file check.
     */
    @Override
    public void run() {
        setRunning(true);

        try {
            // set progress bar
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    final CheckStyleToolWindowPanel toolWindowPanel = toolWindowPanel();
                    if (toolWindowPanel != null) {
                        toolWindowPanel.displayInProgress(getFiles().size());
                    }
                }
            });

            this.processFilesForModuleInfoAndScan();

            // invoke Swing fun in Swing thread.
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    final CheckStyleToolWindowPanel toolWindowPanel = toolWindowPanel();
                    if (toolWindowPanel != null) {
                        toolWindowPanel.displayResults(getFileResults());
                    }
                    markThreadComplete();
                }
            });

        } catch (final Throwable e) {
            final CheckStylePluginException processedError = CheckStylePlugin.processError(
                    "An error occurred during a file scan.", e);

            if (processedError != null) {
                LOG.error("An error occurred while scanning a file.", processedError);

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        final CheckStyleToolWindowPanel toolWindowPanel = toolWindowPanel();
                        if (toolWindowPanel != null) {
                            toolWindowPanel.displayErrorResult(processedError);
                        }
                        markThreadComplete();
                    }
                });
            }
        }
    }

}
