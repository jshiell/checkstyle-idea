package org.infernus.idea.checkstyle.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public final class Async {
    private static final int FIFTY_MS = 50;

    private Async() {
    }

    public static <T> Future<T> executeOnPooledThread(final Callable<T> callable) {
        return ApplicationManager.getApplication().executeOnPooledThread(callable);
    }

    public static <T> Future<T> whenFinished(final Future<T> future) {
        while (!future.isDone() && !future.isCancelled()) {
            ProgressManager.checkCanceled();
            waitFor(FIFTY_MS);
        }
        return future;
    }

    private static void waitFor(final int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
