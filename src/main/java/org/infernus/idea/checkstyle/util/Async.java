package org.infernus.idea.checkstyle.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Async {
    private static final Logger LOG = Logger.getInstance(Async.class);

    private static final int POLL_INTERVAL_MS = 50;

    private Async() {
    }

    @Nullable
    public static <T> T asyncResultOf(@NotNull final Callable<T> callable,
                                      @Nullable final T defaultValue,
                                      final long timeoutInMs) {
        try {
            return whenFinished(executeOnPooledThread(callable), timeoutInMs).get();

        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static <T> Future<T> executeOnPooledThread(final Callable<T> callable) {
        return ApplicationManager.getApplication().executeOnPooledThread(callable);
    }

    public static <T> Future<T> whenFinished(final Future<T> future,
                                             final long timeoutInMs) {
        final long deadline = System.currentTimeMillis() + timeoutInMs;
        while (!future.isCancelled()) {
            ProgressManager.checkCanceled();
            final long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                LOG.debug("Async task exhausted timeout of " + timeoutInMs + "ms, cancelling.");
                future.cancel(true);
                throw new ProcessCanceledException();
            }
            try {
                future.get(Math.min(remaining, POLL_INTERVAL_MS), TimeUnit.MILLISECONDS);
                return future;
            } catch (TimeoutException e) {
                // not yet done; loop to check cancellation and deadline
            } catch (ExecutionException | InterruptedException e) {
                Thread.currentThread().interrupt();
                return future;
            }
        }
        return future;
    }
}
