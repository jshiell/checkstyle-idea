package org.infernus.idea.checkstyle.checker;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStyleModuleConfiguration;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.checks.Check;
import org.infernus.idea.checkstyle.checks.CheckFactory;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;
import org.infernus.idea.checkstyle.util.CheckStyleUtilities;
import org.infernus.idea.checkstyle.util.ScannableFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Runnable for scanning an individual file.
 */
final class FileScanner implements Runnable {

    private static final Log LOG = LogFactory.getLog(FileScanner.class);

    private CheckStylePlugin plugin;
    private Map<PsiFile, List<ProblemDescriptor>> results;
    private Set<PsiFile> filesToScan;
    private ClassLoader moduleClassLoader;
    private Throwable error;

    /**
     * Create a new file scanner.
     *
     * @param checkStylePlugin  CheckStylePlugin.
     * @param filesToScan       the files to scan.
     * @param moduleClassLoader the class loader for the file's module
     */
    public FileScanner(final CheckStylePlugin checkStylePlugin,
                       final Set<PsiFile> filesToScan,
                       final ClassLoader moduleClassLoader) {
        this.plugin = checkStylePlugin;
        this.filesToScan = filesToScan;
        this.moduleClassLoader = moduleClassLoader;
    }

    public void run() {
        try {
            results = checkPsiFile(filesToScan);

            final CheckStyleToolWindowPanel toolWindowPanel = CheckStyleToolWindowPanel.panelFor(plugin.getProject());
            if (toolWindowPanel != null) {
                toolWindowPanel.incrementProgressBarBy(filesToScan.size());
            }
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
     * @param psiFilesToScan the PSI psiFilesToScan to scan. These will be
     *                       ignored if not a java file and not from the same module.
     * @return a list of tree nodes representing the result tree for this
     *         file, an empty list or null if this file is invalid or
     *         has no errors.
     * @throws Throwable if the
     */
    private Map<PsiFile, List<ProblemDescriptor>> checkPsiFile(final Set<PsiFile> psiFilesToScan)
            throws Throwable {
        if (psiFilesToScan == null || psiFilesToScan.isEmpty()) {
            LOG.debug("No elements were specified");
            return null;
        }

        Module module = null;

        final List<ScannableFile> tempFiles = new ArrayList<ScannableFile>();
        final Map<String, PsiFile> filesToElements = new HashMap<String, PsiFile>();

        final boolean checkTestClasses = this.plugin.getConfiguration().isScanningTestClasses();
        final boolean scanOnlyJavaFiles = !plugin.getConfiguration().isScanningNonJavaFiles();

        try {
            final AccessToken readAccessToken = ApplicationManager.getApplication().acquireReadActionLock();
            try {
                for (final PsiFile psiFile : psiFilesToScan) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Processing " + describe(psiFile));
                    }

                    if (psiFile == null || !psiFile.getVirtualFile().isValid() || !psiFile.isPhysical()) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Skipping as invalid type: " + describe(psiFile));
                        }
                        continue;
                    }

                    if (module == null) {
                        module = ModuleUtil.findModuleForPsiElement(psiFile);
                    } else {
                        final Module elementModule = ModuleUtil.findModuleForPsiElement(psiFile);
                        if (!elementModule.equals(module)) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Skipping as modules do not match: " + describe(psiFile)
                                        + " : " + elementModule + " does not match " + module);
                            }
                            continue;
                        }
                    }

                    if (!checkTestClasses && isTestClass(psiFile)) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Skipping test class " + psiFile.getName());
                        }
                        continue;
                    }

                    if (scanOnlyJavaFiles && !CheckStyleUtilities.isJavaFile(psiFile.getFileType())) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Skipping non-Java file " + psiFile.getName());
                        }
                        continue;
                    }

                    final ScannableFile tempFile = createTemporaryFile(psiFile);
                    if (tempFile != null) {
                        tempFiles.add(tempFile);
                        filesToElements.put(tempFile.getAbsolutePath(), psiFile);
                    }
                }
            } finally {
                readAccessToken.finish();
            }

            if (module == null || filesToElements.isEmpty()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No valid files were supplied");
                }
                return null;
            }

            return performCheckStyleScan(module, tempFiles, filesToElements);

        } finally {
            for (final ScannableFile tempFile : tempFiles) {
                if (tempFile != null) {
                    tempFile.deleteIfRequired();
                }
            }
        }
    }

    private String describe(final PsiFile psiFile) {
        if (psiFile != null) {
            return psiFile.getName();
        }
        return null;
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private Map<PsiFile, List<ProblemDescriptor>> performCheckStyleScan(final Module module,
                                                                        final List<ScannableFile> tempFiles,
                                                                        final Map<String, PsiFile> filesToElements) {
        final InspectionManager manager = InspectionManager.getInstance(module.getProject());
        final Checker checker = getChecker(module, moduleClassLoader);
        final Configuration config = getConfig(module);
        if (checker == null || config == null) {
            return Collections.emptyMap();
        }

        final List<Check> checks = CheckFactory.getChecks(config);
        final boolean suppressingErrors = plugin.getConfiguration().isSuppressingErrors();

        final CheckStyleAuditListener listener;
        synchronized (checker) {
            listener = new CheckStyleAuditListener(filesToElements, manager, true, suppressingErrors, checks);
            checker.addListener(listener);
            checker.process(asListOfFiles(tempFiles));
        }

        return listener.getAllProblems();
    }

    private List<File> asListOfFiles(final List<ScannableFile> tempFiles) {
        final List<File> listOfFiles = new ArrayList<File>();
        for (ScannableFile tempFile : tempFiles) {
            listOfFiles.add(tempFile.getFile());
        }
        return listOfFiles;
    }

    private ScannableFile createTemporaryFile(final PsiFile psiFile) {
        ScannableFile tempFile = null;
        try {
            // we need to copy to a file as IntelliJ may not have
            // saved the file recently...
            final CreateScannableFileAction fileAction
                    = new CreateScannableFileAction(psiFile);
            ApplicationManager.getApplication().runReadAction(fileAction);

            // rethrow any error from the thread.
            //noinspection ThrowableResultOfMethodCallIgnored
            if (fileAction.getFailure() != null) {
                throw fileAction.getFailure();
            }

            tempFile = fileAction.getFile();
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

            return getCheckerFactory().getChecker(location, module, classLoader);

        } catch (Exception e) {
            throw new CheckStylePluginException("Couldn't create Checker", e);
        }
    }

    private ConfigurationLocation getConfigurationLocation(final Module module) {
        final ConfigurationLocation location;
        if (module != null) {
            final CheckStyleModuleConfiguration moduleConfiguration
                    = ModuleServiceManager.getService(module, CheckStyleModuleConfiguration.class);
            if (moduleConfiguration == null) {
                throw new IllegalStateException("Couldn't get checkstyle module configuration");
            }

            if (moduleConfiguration.isExcluded()) {
                location = null;
            } else {
                location = moduleConfiguration.getActiveConfiguration();
            }

        } else {
            location = plugin.getConfiguration().getActiveConfiguration();
        }
        return location;
    }

    private CheckerFactory getCheckerFactory() {
        return ServiceManager.getService(CheckerFactory.class);
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

            return getCheckerFactory().getConfig(location, module);

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
