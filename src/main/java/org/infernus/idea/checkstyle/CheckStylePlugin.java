package org.infernus.idea.checkstyle;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.checker.CheckerFactoryCache;
import org.infernus.idea.checkstyle.checker.ConfigurationLocationResult;
import org.infernus.idea.checkstyle.checker.Problem;
import org.infernus.idea.checkstyle.checker.ScanFiles;
import org.infernus.idea.checkstyle.checker.ScannerListener;
import org.infernus.idea.checkstyle.checker.UiFeedbackScannerListener;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Collections.emptyMap;
import static org.infernus.idea.checkstyle.util.Async.executeOnPooledThread;
import static org.infernus.idea.checkstyle.util.Async.whenFinished;

/**
 * Main class for the CheckStyle scanning plug-in.
 */
public final class CheckStylePlugin implements ProjectComponent {

    public static final String ID_PLUGIN = "CheckStyle-IDEA";
    public static final String ID_MODULE_PLUGIN = "CheckStyle-IDEA-Module";

    private static final Log LOG = LogFactory.getLog(CheckStylePlugin.class);

    private final Set<Future<?>> checksInProgress = new HashSet<>();
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

        disableCheckStyleLogging();
    }

    private void disableCheckStyleLogging() {
        new Checkstyle().disableCheckstyleLogging();  // TODO reuse class
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
     * <p>
     * This is only expected to be called from the event thread.
     *
     * @return true if a scan is in progress.
     */
    public boolean isScanInProgress() {
        synchronized (checksInProgress) {
            return !checksInProgress.isEmpty();
        }
    }

    public void projectOpened() {
        LOG.debug("Project opened.");
    }

    public void projectClosed() {
        LOG.debug("Project closed; invalidating checkers.");

        invalidateCheckerCache();
    }

    private void invalidateCheckerCache() {
        ServiceManager.getService(CheckerFactoryCache.class).invalidate();
    }

    @NotNull
    public String getComponentName() {
        return ID_PLUGIN;
    }

    public void initComponent() {
    }

    public void disposeComponent() {
    }

    public static void processErrorAndLog(@NotNull final String action,
                                          @NotNull final Throwable e) {
        final CheckStylePluginException processed = CheckStylePluginException.wrap(e);
        if (processed != null) {
            LOG.error(action + " failed", processed);
        }
    }

    private <T> Future<T> checkInProgress(final Future<T> checkFuture) {
        synchronized (checksInProgress) {
            if (!checkFuture.isDone()) {
                checksInProgress.add(checkFuture);
            }
        }
        return checkFuture;
    }

    public void stopChecks() {
        synchronized (checksInProgress) {
            checksInProgress.forEach(task -> task.cancel(true));
            checksInProgress.clear();
        }
    }

    public <T> void checkComplete(final Future<T> task) {
        if (task == null) {
            return;
        }

        synchronized (checksInProgress) {
            checksInProgress.remove(task);
        }
    }

    public void asyncScanFiles(final List<VirtualFile> files,
                               final ConfigurationLocation overrideConfigLocation) {
        LOG.info("Scanning current file(s).");

        if (files == null || files.isEmpty()) {
            LOG.debug("No files provided.");
            return;
        }

        final ScanFiles checkFiles = new ScanFiles(this, files, overrideConfigLocation);
        checkFiles.addListener(new UiFeedbackScannerListener(this));
        runAsyncCheck(checkFiles);
    }

    public Map<PsiFile, List<Problem>> scanFiles(@NotNull final List<VirtualFile> files) {
        if (files.isEmpty()) {
            return emptyMap();
        }

        try {
            return whenFinished(runAsyncCheck(new ScanFiles(this, files, null))).get();

        } catch (final Throwable e) {
            LOG.error("Error scanning files", e);
            return emptyMap();
        }
    }

    private Future<Map<PsiFile, List<Problem>>> runAsyncCheck(final ScanFiles checker) {
        final Future<Map<PsiFile, List<Problem>>> checkFilesFuture = checkInProgress(executeOnPooledThread(checker));
        checker.addListener(new ScanCompletionTracker(checkFilesFuture));
        return checkFilesFuture;
    }

    public ConfigurationLocation getConfigurationLocation(@Nullable final Module module,
                                                          @Nullable final ConfigurationLocation override) {
        if (override != null) {
            return override;
        }

        if (module != null) {
            final CheckStyleModuleConfiguration moduleConfiguration
                    = ModuleServiceManager.getService(module, CheckStyleModuleConfiguration.class);
            if (moduleConfiguration == null) {
                throw new IllegalStateException("Couldn't get checkstyle module configuration");
            }

            if (moduleConfiguration.isExcluded()) {
                return null;
            }
            return moduleConfiguration.getActiveConfiguration();

        }
        return getConfiguration().getActiveConfiguration();
    }

    private class ScanCompletionTracker implements ScannerListener {
        private final Future<Map<PsiFile, List<Problem>>> future;

        ScanCompletionTracker(final Future<Map<PsiFile, List<Problem>>> future) {
            this.future = future;
        }

        @Override
        public void scanStarting(final List<PsiFile> filesToScan) {
        }

        @Override
        public void filesScanned(final int count) {
        }

        @Override
        public void scanComplete(final ConfigurationLocationResult configurationLocationResult,
                                 final Map<PsiFile, List<Problem>> scanResults) {
            checkComplete(future);
        }

        @Override
        public void errorCaught(final CheckStylePluginException error) {
        }
    }

}
