package org.infernus.idea.checkstyle.checker;

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
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStyleModulePlugin;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.checks.Check;
import org.infernus.idea.checkstyle.checks.CheckFactory;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.util.CheckStyleUtilities;
import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    public FileScanner(final CheckStylePlugin checkStylePlugin,
                       final PsiFile fileToScan,
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

        final boolean checkTestClasses = this.plugin.getConfiguration().isScanningTestClasses();
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
            final Checker checker = getChecker(psiFile, moduleClassLoader);
            final List<Check> checks = CheckFactory.getChecks(getConfig(psiFile));

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

            final Map<String, PsiFile> filesToScan = Collections.singletonMap(tempFile.getAbsolutePath(), psiFile);
            final CheckStyleAuditListener listener
                    = new CheckStyleAuditListener(filesToScan, manager, true, checks);
            checker.addListener(listener);
            checker.process(Arrays.asList(tempFile));

            return listener.getProblems(psiFile);

        } catch (IOException e) {
            LOG.error("Failure when creating temp file", e);
            throw new IllegalStateException("Couldn't create temp file", e);

        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }


    /**
     * Produce a CheckStyle checker.
     *
     * @param psiFile     the file to be checked.
     * @param classLoader CheckStyle classloader or null if default
     *                    should be used.
     * @return a checker.
     */
    private Checker getChecker(final PsiFile psiFile,
                               final ClassLoader classLoader) {
        LOG.debug("Getting CheckStyle checker.");

        try {
            final Module module = ModuleUtil.findModuleForPsiElement(psiFile);

            final ConfigurationLocation location = getConfigurationLocation(module);
            if (location == null) {
                return null;
            }

            File baseDir = location.getBaseDir();
            if (baseDir == null) {
                baseDir = new File(plugin.getProject().getBaseDir().getPath());
            }

            return CheckerFactory.getInstance().getChecker(location, baseDir, classLoader);

        } catch (Throwable e) {
            throw new CheckStylePluginException("Couldn't create Checker", e);
        }
    }

    private ConfigurationLocation getConfigurationLocation(final Module module) {
        final ConfigurationLocation location;
        if (module != null) {
            final CheckStyleModulePlugin checkStyleModulePlugin = module.getComponent(CheckStyleModulePlugin.class);
            if (checkStyleModulePlugin == null) {
                throw new IllegalStateException("Couldn't get checkstyle module plugin");
            }
            location = checkStyleModulePlugin.getConfiguration().getActiveConfiguration();

        } else {
            location = plugin.getConfiguration().getActiveConfiguration();
        }
        return location;
    }

    /**
     * Retrieve a CheckStyle configuration.
     *
     * @param psiFile the file to be checked.
     * @return a checkstyle configuration.
     */
    private Configuration getConfig(final PsiFile psiFile) {
        LOG.debug("Getting CheckStyle checker.");

        try {
            final Module module = ModuleUtil.findModuleForPsiElement(psiFile);

            final ConfigurationLocation location = getConfigurationLocation(module);
            if (location == null) {
                return null;
            }

            return CheckerFactory.getInstance().getConfig(location);

        } catch (Throwable e) {
            throw new CheckStylePluginException("Couldn't create Checker", e);
        }
    }

    private boolean isTestClass(final PsiElement element) {
        final VirtualFile elementFile = element.getContainingFile().getVirtualFile();
        if (elementFile == null) {
            return false;
        }

        final Module module = ModuleUtil.findModuleForFile(elementFile, plugin.getProject());
        if (module == null) {
            return false;
        }

        final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
        return moduleRootManager != null && moduleRootManager.getFileIndex().isInTestSourceContent(elementFile);
    }
}
