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
import java.util.*;

/**
 * Runnable for scanning an individual file.
 */
final class FileScanner implements Runnable {

    private CheckStylePlugin plugin;
    private Map<PsiFile, List<ProblemDescriptor>> results;
    private List<PsiFile> filesToScan;
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
        this(checkStylePlugin, Arrays.asList(fileToScan), moduleClassLoader);
    }

    /**
     * Create a new file scanner.
     *
     * @param checkStylePlugin  CheckStylePlugin.
     * @param filesToScan       the files to scan.
     * @param moduleClassLoader the class loader for the file's module
     */
    public FileScanner(final CheckStylePlugin checkStylePlugin,
                       final List<PsiFile> filesToScan,
                       final ClassLoader moduleClassLoader) {
        this.plugin = checkStylePlugin;
        this.filesToScan = filesToScan;
        this.moduleClassLoader = moduleClassLoader;
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
        try {
            results = checkPsiFile(filesToScan, moduleClassLoader);

            this.plugin.getToolWindowPanel().incrementProgressBarBy(filesToScan.size());
        } catch (Throwable e) {
            error = e;
        }
    }

    /**
     * Get the results of the scan.
     *
     * @return the results of the scan.
     */
    public Map<PsiFile, List<ProblemDescriptor>> getResults() {
        if (results != null) {
            return Collections.unmodifiableMap(results);
        }

        return Collections.emptyMap();
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
     * @param psiFilesToScan    the PSI psiFilesToScan to scan. Thezse will be
     *                          ignored if not a java file and not from the same module.
     * @param moduleClassLoader the class loader for the current module.
     * @return a list of tree nodes representing the result tree for this
     *         file, an empty list or null if this file is invalid or
     *         has no errors.
     * @throws Throwable if the
     */
    private Map<PsiFile, List<ProblemDescriptor>> checkPsiFile(final List<PsiFile> psiFilesToScan,
                                                               final ClassLoader moduleClassLoader)
            throws Throwable {
        if (psiFilesToScan == null || psiFilesToScan.isEmpty()) {
            LOG.debug("No elements were specified");
            return null;
        }

        Module module = null;

        final List<File> tempFiles = new ArrayList<File>();
        final Map<String, PsiFile> filesToElements = new HashMap<String, PsiFile>();

        final boolean checkTestClasses = this.plugin.getConfiguration().isScanningTestClasses();

        try {
            for (final PsiFile psiFile : psiFilesToScan) {
                final String fileDescription = (psiFile != null ? psiFile.getName() : null);
                LOG.debug("Processing " + fileDescription);

                if (psiFile == null || !psiFile.isValid() || !psiFile.isPhysical()) {
                    LOG.debug("Skipping as invalid type: " + fileDescription);
                    continue;
                }

                if (module == null) {
                    module = ModuleUtil.findModuleForPsiElement(psiFile);
                } else {
                    final Module elementModule = ModuleUtil.findModuleForPsiElement(psiFile);
                    if (!elementModule.equals(module)) {
                        LOG.debug("Skipping as modules do not match: " + fileDescription + " : " + elementModule
                                + " does not match " + module);
                        continue;
                    }
                }

                if (!checkTestClasses && isTestClass(psiFile)) {
                    LOG.debug("Skipping test class " + psiFile.getName());
                    continue;
                }

                if (!CheckStyleUtilities.isValidFileType(psiFile.getFileType())) {
                    LOG.debug("Skipping invalid file type " + psiFile.getName());
                    continue;
                }

                final File tempFile = createTemporaryFile(psiFile);
                if (tempFile != null) {
                    tempFiles.add(tempFile);
                    filesToElements.put(tempFile.getAbsolutePath(), psiFile);
                }
            }

            if (module == null || filesToElements.size() == 0) {
                LOG.debug("No valid files were supplied");
                return null;
            }

            return performCheckStyleScan(moduleClassLoader, module, tempFiles, filesToElements);

        } finally {
            for (final File tempFile : tempFiles) {
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }
            }
        }
    }

    private Map<PsiFile, List<ProblemDescriptor>> performCheckStyleScan(final ClassLoader moduleClassLoader,
                                                                        final Module module,
                                                                        final List<File> tempFiles,
                                                                        final Map<String, PsiFile> filesToElements) {
        final InspectionManager manager = InspectionManager.getInstance(module.getProject());
        final Checker checker = getChecker(module, moduleClassLoader);
        final List<Check> checks = CheckFactory.getChecks(getConfig(module));

        final CheckStyleAuditListener listener
                = new CheckStyleAuditListener(filesToElements, manager, true, checks);
        checker.addListener(listener);
        checker.process(tempFiles);

        return listener.getAllProblems();
    }

    private File createTemporaryFile(final PsiFile psiFile) {
        File tempFile = null;
        try {
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

        } catch (IOException e) {
            LOG.error("Failure when creating temp file", e);
        }

        return tempFile;
    }


    /**
     * Produce a CheckStyle checker.
     *
     * @param module      the module the checked file(s) belong to.
     * @param classLoader CheckStyle classloader or null if default
     *                    should be used.
     * @return a checker.
     */

    private Checker getChecker(final Module module,
                               final ClassLoader classLoader) {
        LOG.debug("Getting CheckStyle checker.");

        try {
            final ConfigurationLocation location = getConfigurationLocation(module);
            if (location == null) {
                return null;
            }

            return CheckerFactory.getInstance().getChecker(location, module, classLoader);

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
     * @param module the module to fetch configuration for.
     * @return a checkstyle configuration.
     */
    private Configuration getConfig(final Module module) {
        LOG.debug("Getting CheckStyle checker.");

        try {
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
