package org.infernus.idea.checkstyle;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.puppycrawl.tools.checkstyle.Checker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.util.CheckStyleUtilities;
import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Runnable for scanning an individual file.
 */
final class FileScanner implements Runnable {

    private CheckStylePlugin plugin;
    private List<ProblemDescriptor> results;
    private PsiFile fileToScan;
    private ClassLoader moduleClassLoader;
    private Throwable error;

    /**
     * Logger for this class.
     */
    @NonNls
    private static final Log LOG = LogFactory.getLog(FileScanner.class);

    /**
     * Create a new file scanner.
     *
     * @param checkStylePlugin  CheckStylePlugin.
     * @param fileToScan        the file to scan.
     * @param moduleClassLoader the class loader for the file's module
     */
    public FileScanner(CheckStylePlugin checkStylePlugin, final PsiFile fileToScan,
                       final ClassLoader moduleClassLoader) {
        this.plugin = checkStylePlugin;
        this.fileToScan = fileToScan;
        this.moduleClassLoader = moduleClassLoader;
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
        try {
            results = checkPsiFile(fileToScan, moduleClassLoader);

            this.plugin.getToolWindowPanel().incrementProgressBar();
        } catch (Throwable e) {
            error = e;
        }
    }

    /**
     * Get the results of the scan.
     *
     * @return the results of the scan.
     */
    public List<ProblemDescriptor> getResults() {
        return results;
    }

    /**
     * Get any error that may have occurred during the scan.
     *
     * @return any error that may have occurred during the scan
     */
    public Throwable getError() {
        return error;
    }

    /**
     * Scan a PSI file with CheckStyle.
     *
     * @param element           the PSI element to scan. This will be
     *                          ignored if not a java file.
     * @param moduleClassLoader the class loader for the current module.
     * @return a list of tree nodes representing the result tree for this
     *         file, an empty list or null if this file is invalid or
     *         has no errors.
     * @throws Throwable if the
     */
    private List<ProblemDescriptor> checkPsiFile(final PsiElement element,
                                                 final ClassLoader moduleClassLoader)
            throws Throwable {
        if (element == null || !element.isValid() || !element.isPhysical()
                || !PsiFile.class.isAssignableFrom(element.getClass())) {
            final String elementString = (element != null
                    ? element.toString() : null);
            LOG.debug("Skipping as invalid type: " + elementString);

            return null;
        }

        final PsiFile psiFile = (PsiFile) element;
        LOG.debug("Scanning " + psiFile.getName());

        final boolean checkTestClasses = this.plugin.configuration.isScanningTestClasses();
        if (!checkTestClasses && isTestClass(element)) {
            LOG.debug("Skipping test class " + psiFile.getName());
            return null;
        }

        final InspectionManager manager
                = InspectionManager.getInstance(psiFile.getProject());

        if (!CheckStyleUtilities.isValidFileType(psiFile.getFileType())) {
            return null;
        }

        File tempFile = null;
        try {
            final Checker checker = this.plugin.getChecker(moduleClassLoader);

            // we need to copy to a file as IntelliJ may not have
            // saved the file recently...
            final CreateTempFileThread fileThread
                    = new CreateTempFileThread(psiFile);
            ApplicationManager.getApplication().runReadAction(fileThread);

            // rethrow any error from the thread.
            if (fileThread.getFailure() != null) {
                throw fileThread.getFailure();
            }

            tempFile = fileThread.getFile();
            if (tempFile == null) {
                throw new IllegalStateException("Failed to create temporary file.");
            }

            final CheckStyleAuditListener listener
                    = new CheckStyleAuditListener(psiFile, manager, true);
            checker.addListener(listener);
            checker.process(new File[]{tempFile});
            checker.destroy();

            return listener.getProblems();

        } catch (IOException e) {
            LOG.error("Failure when creating temp file", e);
            throw new IllegalStateException("Couldn't create temp file", e);

        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private boolean isTestClass(final PsiElement element) {
        final VirtualFile elementFile = element.getContainingFile().getVirtualFile();
        if (elementFile == null) {
            return false;
        }

        final Module module = ModuleUtil.findModuleForFile(elementFile, this.plugin.project);
        if (module == null) {
            return false;
        }

        final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
        return moduleRootManager != null && moduleRootManager.getFileIndex().isInTestSourceContent(elementFile);
    }
}
