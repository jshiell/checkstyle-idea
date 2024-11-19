package org.infernus.idea.checkstyle;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.checker.*;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ScanResult;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.Future;

import static org.infernus.idea.checkstyle.util.Async.executeOnPooledThread;
import static org.infernus.idea.checkstyle.util.Async.whenFinished;

public class StaticScanner {
    private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(StaticScanner.class);

    private static final long NO_TIMEOUT = 0L;

    private final Set<Future<?>> checksInProgress = new HashSet<>();
    private final Project project;

    public StaticScanner(@NotNull final Project project) {
        this.project = project;
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
        LOG.debug("Scanning current file(s).");

        if (files == null || files.isEmpty()) {
            LOG.debug("No files provided.");
            return;
        }

        final ScanFiles checkFiles = new ScanFiles(project, files, overrideConfigLocation);
        checkFiles.addListener(new UiFeedbackScannerListener(project));
        runAsyncCheck(checkFiles);
    }

    public ScanResult scanFiles(@NotNull final List<VirtualFile> files) {
        if (files.isEmpty()) {
            return ScanResult.EMPTY;
        }

        try {
            return whenFinished(runAsyncCheck(new ScanFiles(project, files, null)), NO_TIMEOUT).get();
        } catch (final Throwable e) {
            LOG.warn("Error scanning files", e);
            return ScanResult.EMPTY;
        }
    }

    private Future<ScanResult> runAsyncCheck(final ScanFiles checker) {
        final Future<ScanResult> checkFilesFuture = checkInProgress(executeOnPooledThread(checker));
        checker.addListener(new ScanCompletionTracker(checkFilesFuture));
        return checkFilesFuture;
    }

    private class ScanCompletionTracker implements ScannerListener {

        private final Future<ScanResult> future;

        ScanCompletionTracker(final Future<ScanResult> future) {
            this.future = future;
        }

        @Override
        public void scanStarting(final List<PsiFile> filesToScan) {
        }

        @Override
        public void filesScanned(final int count) {
        }

        @Override
        public void scanCompletedSuccessfully(final ScanResult scanResult) {
            checkComplete(future);
        }

        @Override
        public void scanFailedWithError(final CheckStylePluginException error) {
            checkComplete(future);
        }
    }

}
