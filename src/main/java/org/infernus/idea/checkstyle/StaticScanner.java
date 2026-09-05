package org.infernus.idea.checkstyle;

import com.intellij.openapi.Disposable;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.infernus.idea.checkstyle.util.Async.NO_TIMEOUT;
import static org.infernus.idea.checkstyle.util.Async.executeOnPooledThread;
import static org.infernus.idea.checkstyle.util.Async.whenFinished;

public class StaticScanner implements Disposable {
    private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(StaticScanner.class);

    // Tracked only so stopChecks() can cancel outstanding work; "is a scan in progress" is driven
    // solely by activeScans below, since that never has to guess whether a Future is done yet.
    private final Set<Future<?>> checksInProgress = new HashSet<>();
    private final AtomicInteger activeScans = new AtomicInteger();
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
        return activeScans.get() > 0;
    }

    public void stopChecks() {
        synchronized (checksInProgress) {
            checksInProgress.forEach(task -> task.cancel(true));
            checksInProgress.clear();
        }
        activeScans.set(0);
    }

    @Override
    public void dispose() {
        stopChecks();
    }

    public void asyncScanFiles(final List<VirtualFile> files, final ConfigurationLocation overrideConfigLocation) {
        asyncScanFiles(files, overrideConfigLocation, ResultHandling.REPLACE);
    }

    public void asyncScanFiles(final List<VirtualFile> files,
                               final ConfigurationLocation overrideConfigLocation,
                               final ResultHandling resultHandling) {
        LOG.debug("Scanning current file(s).");

        if (files == null) {
            LOG.debug("No files provided.");
            return;
        }

        final ScanFiles checkFiles = new ScanFiles(project, files, overrideConfigLocation);
        checkFiles.addListener(new UiFeedbackScannerListener(project, resultHandling));
        runAsyncCheck(checkFiles);
    }

    public List<ScanResult> scanFiles(@NotNull final List<VirtualFile> files) {
        if (files.isEmpty()) {
            return List.of(ScanResult.EMPTY);
        }

        try {
            return whenFinished(runAsyncCheck(new ScanFiles(project, files, null)), NO_TIMEOUT).get();
        } catch (final Throwable e) {
            LOG.warn("Error scanning files", e);
            return List.of(ScanResult.EMPTY);
        }
    }

    private Future<List<ScanResult>> runAsyncCheck(final ScanFiles checker) {
        // Mark the scan pending and register the completion listener *before* submitting the checker
        // to the pool. A fast scan (e.g. an empty file list) can otherwise run to completion, and fire
        // its listeners, before this method finishes its own bookkeeping - stranding the scan as
        // "not in progress" even though it was genuinely dispatched.
        activeScans.incrementAndGet();
        checker.addListener(new ScanCompletionTracker());

        final Future<List<ScanResult>> future = executeOnPooledThread(checker);
        synchronized (checksInProgress) {
            checksInProgress.removeIf(Future::isDone);
            checksInProgress.add(future);
        }
        return future;
    }

    private class ScanCompletionTracker implements ScannerListener {

        @Override
        public void scanStarting(final List<PsiFile> filesToScan) {
        }

        @Override
        public void filesScanned(final int count) {
        }

        @Override
        public void scanCompletedSuccessfully(final List<ScanResult> scanResults) {
            activeScans.updateAndGet(count -> Math.max(0, count - 1));
        }

        @Override
        public void scanFailedWithError(final CheckStylePluginException error) {
            activeScans.updateAndGet(count -> Math.max(0, count - 1));
        }
    }

}
