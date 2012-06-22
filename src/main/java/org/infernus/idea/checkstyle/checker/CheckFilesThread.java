package org.infernus.idea.checkstyle.checker;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.util.ModuleClassPathBuilder;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;

public class CheckFilesThread extends AbstractCheckerThread {

    /**
     * Logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(CheckFilesThread.class);

    /**
     * Create a thread to check the given files.
     *
     * @param checkStylePlugin       CheckStylePlugin.
     * @param moduleClassPathBuilder the class path builder.
     * @param virtualFiles           the files to check.
     */
    public CheckFilesThread(final CheckStylePlugin checkStylePlugin,
                            final ModuleClassPathBuilder moduleClassPathBuilder,
                            final List<VirtualFile> virtualFiles) {
        super(checkStylePlugin, moduleClassPathBuilder, virtualFiles);
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
                    getPlugin().getToolWindowPanel().setProgressBarMax(getFiles().size());
                    getPlugin().getToolWindowPanel().displayInProgress();
                }
            });

            this.processFilesForModuleInfoAndScan();

            // invoke Swing fun in Swing thread.
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    getPlugin().getToolWindowPanel().displayResults(getFileResults());
                    getPlugin().getToolWindowPanel().expandTree();
                    getPlugin().getToolWindowPanel().clearProgressBar();
                    getPlugin().getToolWindowPanel().setProgressText(null);

                    getPlugin().setThreadComplete(CheckFilesThread.this);
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
                        getPlugin().getToolWindowPanel().displayErrorResult(processedError);
                        getPlugin().getToolWindowPanel().clearProgressBar();
                        getPlugin().getToolWindowPanel().setProgressText(null);

                        getPlugin().setThreadComplete(CheckFilesThread.this);
                    }
                });
            }
        }
    }

}
