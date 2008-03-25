package org.infernus.idea.checkstyle;

import com.intellij.psi.PsiFile;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.codeInspection.ProblemDescriptor;

import javax.swing.*;
import java.util.List;
import java.util.HashMap;
import java.lang.reflect.InvocationTargetException;

import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.jetbrains.annotations.NonNls;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class CheckFilesThread extends AbstractCheckerThread {

    /**
     * Logger for this class.
     */
    @NonNls
    private static final Log LOG = LogFactory.getLog(CheckFilesThread.class);
    
    /**
     * Create a thread to check the given files.
     *
     * @param checkStylePlugin CheckStylePlugin.
     * @param virtualFiles the files to check.
     */
    public CheckFilesThread(CheckStylePlugin checkStylePlugin, final List<VirtualFile> virtualFiles) {
        super(checkStylePlugin, virtualFiles);
        this.fileResults = new HashMap<PsiFile, List<ProblemDescriptor>>();
    }

    public void runFileScanner(FileScanner fileScanner) throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(fileScanner);
    }

    /**
     * Execute the file check.
     */
    public void run() {
        try {
            // set progress bar
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    CheckFilesThread.this.plugin.getToolWindowPanel().setProgressBarMax(files.size());
                    CheckFilesThread.this.plugin.getToolWindowPanel().displayInProgress();
                }
            });

            this.processFilesForModuleInfoAndScan();

            // invoke Swing fun in Swing thread.
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    CheckFilesThread.this.plugin.getToolWindowPanel().displayResults(fileResults);
                    CheckFilesThread.this.plugin.getToolWindowPanel().expandTree();
                    CheckFilesThread.this.plugin.getToolWindowPanel().clearProgressBar();
                    CheckFilesThread.this.plugin.getToolWindowPanel().setProgressText(null);

                    CheckFilesThread.this.plugin.scanInProgress = false;
                }
            });

        } catch (final Throwable e) {
            final CheckStylePluginException processedError = CheckStylePlugin.processError(
                    "An error occurred during a file scan.", e);

            if (processedError != null) {
                LOG.error("An error occurred while scanning a file.",
                        processedError);

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        CheckFilesThread.this.plugin.getToolWindowPanel().displayErrorResult(processedError);
                        CheckFilesThread.this.plugin.getToolWindowPanel().clearProgressBar();
                        CheckFilesThread.this.plugin.getToolWindowPanel().setProgressText(null);

                        CheckFilesThread.this.plugin.scanInProgress = false;
                    }
                });
            }
        }
    }

}
