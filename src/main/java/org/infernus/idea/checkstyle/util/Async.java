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

    /**
     * Sentinel value for {@link #whenFinished} indicating no timeout: the future will be awaited
     * indefinitely (only IDEA progress cancellation applies).
     * <p>
     * Previously this was {@code 0L}, which caused {@code deadline = now + 0}, so the deadline was
     * immediately in the past and the future was cancelled on the first iteration.
     */
    public static final long NO_TIMEOUT = -1L;

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
        final boolean hasTimeout = timeoutInMs > 0;
        final long deadline = hasTimeout ? System.currentTimeMillis() + timeoutInMs : Long.MAX_VALUE;
        while (!future.isCancelled()) {
            ProgressManager.checkCanceled();
            if (hasTimeout) {
                final long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    LOG.debug("Async task exhausted timeout of " + timeoutInMs + "ms, cancelling.");
                    future.cancel(true);
                    throw new ProcessCanceledException();
                }
            }
            try {
                final long pollMs = hasTimeout
                        ? Math.min(deadline - System.currentTimeMillis(), POLL_INTERVAL_MS)
                        : POLL_INTERVAL_MS;
                future.get(Math.max(pollMs, 1), TimeUnit.MILLISECONDS);
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
