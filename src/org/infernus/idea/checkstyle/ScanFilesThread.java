package org.infernus.idea.checkstyle;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.codeInspection.ProblemDescriptor;

import java.util.List;
import java.util.Map;
import java.lang.reflect.InvocationTargetException;

import org.jetbrains.annotations.NonNls;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;

public class ScanFilesThread extends AbstractCheckerThread {

    /**
     * Logger for this class.
     */
    @NonNls
    private static final Log LOG = LogFactory.getLog(ScanFilesThread.class);

    /**
     * Scan Files and store results.
     * @param checkStylePlugin reference to the CheckStylePlugin
     * @param vFiles files to scan 
     * @param results Map to store scan results
     */
    public ScanFilesThread(CheckStylePlugin checkStylePlugin, final List<VirtualFile> vFiles, Map<PsiFile, List<ProblemDescriptor>> results) {
        super(checkStylePlugin, vFiles);
        this.fileResults = results;
    }

    /**
     * Run scan against files.
     */
    public void run() {
        try {
            this.processFilesForModuleInfoAndScan();
        } catch (final Throwable e) {
            final CheckStylePluginException processedError = CheckStylePlugin.processError(
                    "An error occurred during a file scan.", e);

            if (processedError != null) {
                LOG.error("An error occurred while scanning a file.",
                        processedError);
            }
        }
    }


    public void runFileScanner(FileScanner fileScanner) throws InterruptedException, InvocationTargetException {
        fileScanner.run();
    }

}
