package org.infernus.idea.checkstyle;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.checker.AbstractCheckerThread;
import org.infernus.idea.checkstyle.checker.CheckFilesThread;
import org.infernus.idea.checkstyle.checker.ScanFilesThread;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.util.ModuleClassPathBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Main class for the CheckStyle scanning plug-in.
 */
public final class CheckStylePlugin implements ProjectComponent {
    private static final Log LOG = LogFactory.getLog(CheckStylePlugin.class);

    private final Set<AbstractCheckerThread> checksInProgress = new HashSet<AbstractCheckerThread>();
    private final Project project;
    private final CheckStyleConfiguration configuration;

    /**
     * Construct a plug-in instance for the given project.
     *
     * @param project the current project.
     */
    public CheckStylePlugin(@NotNull final Project project) {
        this.project = project;
        this.configuration = ServiceManager.getService(project, CheckStyleConfiguration.class);

        LOG.info("CheckStyle Plugin loaded with project base dir: \"" + getProjectPath() + "\"");
    }

    public Project getProject() {
        return project;
    }

    @Nullable
    private File getProjectPath() {
        final VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) {
            return null;
        }

        return new File(baseDir.getPath());
    }

    /**
     * Get the plugin configuration.
     *
     * @return the plug-in configuration.
     */
    public CheckStyleConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Is a scan in progress?
     * <p/>
     * This is only expected to be called from the event thread.
     *
     * @return true if a scan is in progress.
     */
    public boolean isScanInProgress() {
        synchronized (checksInProgress) {
            return checksInProgress.size() > 0;
        }
    }

    public void projectOpened() {
        LOG.debug("Project opened.");
    }

    public void projectClosed() {
        LOG.debug("Project closed.");
    }

    @NotNull
    public String getComponentName() {
        return CheckStyleConstants.ID_PLUGIN;
    }

    public void initComponent() {
    }

    public void disposeComponent() {
    }

    /**
     * Process an error.
     *
     * @param message a description of the error. May be null.
     * @param error   the exception.
     * @return any exception to be passed upwards.
     */
    public static CheckStylePluginException processError(@Nullable final String message,
                                                         @NotNull final Throwable error) {
        Throwable root = error;
        while (root.getCause() != null
                && !(root instanceof CheckstyleException)) {
            root = root.getCause();
        }

        if (message != null) {
            return new CheckStylePluginException(message, root);
        }

        return new CheckStylePluginException(root.getMessage(), root);
    }

    /**
     * Run a scan on the currently selected file.
     *
     * @param files the files to check.
     */
    public void checkFiles(final List<VirtualFile> files) {
        LOG.info("Scanning current file(s).");

        if (files == null) {
            LOG.debug("No files provided.");
            return;
        }

        final CheckFilesThread checkFilesThread = new CheckFilesThread(this, moduleClassPathBuilder(), files);
        checkFilesThread.setPriority(Thread.MIN_PRIORITY);

        synchronized (checksInProgress) {
            checksInProgress.add(checkFilesThread);
        }

        checkFilesThread.start();
    }

    private ModuleClassPathBuilder moduleClassPathBuilder() {
        return ServiceManager.getService(project, ModuleClassPathBuilder.class);
    }

    /**
     * Stop any checks in progress.
     */
    public void stopChecks() {
        synchronized (checksInProgress) {
            for (final AbstractCheckerThread thread : checksInProgress) {
                thread.stopCheck();
            }

            checksInProgress.clear();
        }
    }

    /**
     * Mark a thread as complete.
     *
     * @param thread the thread to mark.
     */
    public void setThreadComplete(final AbstractCheckerThread thread) {
        if (thread == null) {
            return;
        }

        synchronized (checksInProgress) {
            checksInProgress.remove(thread);
        }
    }

    public Map<PsiFile, List<ProblemDescriptor>> scanFiles(final List<VirtualFile> files,
                                                           final Map<PsiFile, List<ProblemDescriptor>> results) {
        LOG.info("Scanning current file(s).");
        if (files == null) {
            LOG.debug("No files provided.");
            return results;
        }
        final ScanFilesThread scanFilesThread = new ScanFilesThread(this, moduleClassPathBuilder(), files, results);

        synchronized (checksInProgress) {
            checksInProgress.add(scanFilesThread);
        }

        scanFilesThread.start();
        try {
            scanFilesThread.join();

        } catch (final Throwable e) {
            LOG.error("Error scanning files");

        } finally {
            synchronized (checksInProgress) {
                checksInProgress.remove(scanFilesThread);
            }
        }
        return results;
    }

}
