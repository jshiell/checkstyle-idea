package org.infernus.idea.checkstyle;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.apache.log4j.Level;
import org.infernus.idea.checkstyle.checker.*;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.Future;

import static org.infernus.idea.checkstyle.util.Async.executeOnPooledThread;
import static org.infernus.idea.checkstyle.util.Async.whenFinished;


/**
 * Main class for the CheckStyle scanning plug-in.
 */
public final class CheckStylePlugin implements ProjectComponent {

    /**
     * The plugin ID. Caution: It must be identical to the String set in build.gradle at intellij.pluginName
     */
    public static final String ID_PLUGIN = "CheckStyle-IDEA";

    static final String ID_MODULE_PLUGIN = "CheckStyle-IDEA-Module";

    private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(CheckStylePlugin.class);

    private static final long NO_TIMEOUT = 0L;

    private final Set<Future<?>> checksInProgress = new HashSet<>();
    private final Project project;
    private final PluginConfigurationManager configurationManager;
    private final CheckerFactoryCache checkerFactoryCache;


    /**
     * Construct a plug-in instance for the given project.
     *
     * @param project the current project.
     */
    public CheckStylePlugin(@NotNull final Project project,
                            @NotNull final PluginConfigurationManager configurationManager,
                            @NotNull final CheckerFactoryCache checkerFactoryCache) {
        this.project = project;
        this.configurationManager = configurationManager;
        this.checkerFactoryCache = checkerFactoryCache;

        LOG.info("CheckStyle Plugin");

        disableCheckStyleLogging();
    }

    private void disableCheckStyleLogging() {
        try {
            // This is a nasty hack to get around IDEA's DialogAppender sending any errors to the Event Log,
            // which would result in CheckStyle parse errors spamming the Event Log.
            org.apache.log4j.Logger.getLogger("com.puppycrawl.tools.checkstyle.TreeWalker").setLevel(Level.OFF);
        } catch (Exception e) {
            LOG.warn("Unable to suppress logging from CheckStyle's TreeWalker", e);
        }
    }

    public Project getProject() {
        return project;
    }

    /**
     * Get the plugin configuration.
     *
     * @return the plug-in configuration.
     */
    public PluginConfigurationManager configurationManager() {
        return configurationManager;
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

    public static String currentPluginVersion() {
        final IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId(ID_PLUGIN));
        if (plugin != null) {
            return plugin.getVersion();
        }
        return "unknown";
    }

    public void projectClosed() {
        LOG.debug("Project closed; invalidating checkers.");

        checkerFactoryCache.invalidate();
    }

    @NotNull
    public String getComponentName() {
        return ID_PLUGIN;
    }

    public void initComponent() {
    }

    public void disposeComponent() {
    }

    public static void processErrorAndLog(@NotNull final String action, @NotNull final Throwable e) {
        LOG.warn(action + " failed", e);
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

    private <T> void checkComplete(final Future<T> task) {
        if (task == null) {
            return;
        }

        synchronized (checksInProgress) {
            checksInProgress.remove(task);
        }
    }

    public void asyncScanFiles(final List<VirtualFile> files, final ConfigurationLocation overrideConfigLocation) {
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
            return Collections.emptyMap();
        }

        try {
            return whenFinished(runAsyncCheck(new ScanFiles(this, files, null)), NO_TIMEOUT).get();
        } catch (final Throwable e) {
            LOG.warn("Error scanning files", e);
            return Collections.emptyMap();
        }
    }

    private Future<Map<PsiFile, List<Problem>>> runAsyncCheck(final ScanFiles checker) {
        final Future<Map<PsiFile, List<Problem>>> checkFilesFuture = checkInProgress(executeOnPooledThread(checker));
        checker.addListener(new ScanCompletionTracker(checkFilesFuture));
        return checkFilesFuture;
    }

    public ConfigurationLocation getConfigurationLocation(@Nullable final Module module, @Nullable final
    ConfigurationLocation override) {
        if (override != null) {
            return override;
        }

        if (module != null) {
            final CheckStyleModuleConfiguration moduleConfiguration = ModuleServiceManager.getService(module,
                    CheckStyleModuleConfiguration.class);
            if (moduleConfiguration == null) {
                throw new IllegalStateException("Couldn't get checkstyle module configuration");
            }

            if (moduleConfiguration.isExcluded()) {
                return null;
            }
            return moduleConfiguration.getActiveConfiguration();
        }
        return configurationManager().getCurrent().getActiveLocation();
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
        public void scanCompletedSuccessfully(final ConfigurationLocationResult configurationLocationResult,
                                              final Map<PsiFile, List<Problem>> scanResults) {
            checkComplete(future);
        }

        @Override
        public void scanFailedWithError(final CheckStylePluginException error) {
            checkComplete(future);
        }
    }
}
