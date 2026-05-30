package org.infernus.idea.checkstyle.util;

import com.intellij.openapi.progress.ProcessCanceledException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link Async#whenFinished}.
 * <p>
 * Note: these tests do not call {@code Async.executeOnPooledThread()} because that
 * requires a running IntelliJ application. {@link CompletableFuture} is used instead
 * to exercise the waiting/timeout logic in isolation.
 * <p>
 * {@code Async.whenFinished()} internally calls {@code ProgressManager.checkCanceled()}
 * on each poll iteration. In a unit test environment without a running platform,
 * this is expected to be a safe no-op (no active progress indicator).
 */
public class AsyncWhenFinishedTest {

    @Test
    public void whenFinishedReturnsImmediatelyForAlreadyCompletedFuture() throws Exception {
        final CompletableFuture<String> future = CompletableFuture.completedFuture("result");

        final Future<String> returned = Async.whenFinished(future, 1_000);

        assertThat(returned.get(), is("result"));
    }

    @Test
    public void whenFinishedWithTimeoutCancelsFutureAfterDeadlineExpires() {
        // A future that never completes — will time out
        final CompletableFuture<String> future = new CompletableFuture<>();

        // Use a very short timeout (100ms) and verify the future is cancelled
        assertThrows(ProcessCanceledException.class,
                () -> Async.whenFinished(future, 100));

        assertThat("Future should have been cancelled after timeout", future.isCancelled(), is(true));
    }

    @Test
    public void whenFinishedWithNoTimeoutWaitsUntilFutureCompletes() throws Exception {
        final CompletableFuture<String> future = new CompletableFuture<>();

        // Complete the future on a background thread after a short delay
        Thread completerThread = new Thread(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            future.complete("done");
        });
        completerThread.setDaemon(true);
        completerThread.start();

        final Future<String> returned = Async.whenFinished(future, Async.NO_TIMEOUT);

        assertThat(returned.get(2, TimeUnit.SECONDS), is("done"));
        assertThat("Future should NOT be cancelled when NO_TIMEOUT is used", future.isCancelled(), is(false));
    }

    @Test
    public void noTimeoutSentinelIsNegativeOne() {
        // Regression guard: the old sentinel was 0L which caused instant cancellation
        assertThat(Async.NO_TIMEOUT, is(-1L));
    }
}
