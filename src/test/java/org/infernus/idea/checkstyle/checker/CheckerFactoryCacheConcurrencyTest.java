package org.infernus.idea.checkstyle.checker;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.Mockito.mock;

/**
 * Concurrency and stress tests for {@link CheckerFactoryCache}.
 * <p>
 * These tests verify that concurrent calls to {@code invalidate()} and
 * {@code cleanUpExpiredCachedCheckers()} (triggered via {@code get()}) do not
 * result in a checker being destroyed more than once.
 */
public class CheckerFactoryCacheConcurrencyTest {

    /**
     * Verify that concurrent {@code invalidate()} and {@code get()} (which triggers cleanup)
     * calls do not destroy any single checker more than once.
     */
    @Test
    public void checkerIsNotDestroyedMoreThanOnceConcurrently() throws Exception {
        final int threadCount = 10;
        final int iterations = 20;

        for (int iter = 0; iter < iterations; iter++) {
            final CheckerFactoryCache cache = new CheckerFactoryCache();
            final AtomicInteger destroyCount = new AtomicInteger(0);

            // Install a checker that counts destroy() calls
            final CheckStyleChecker mockChecker = mock(CheckStyleChecker.class);
            org.mockito.Mockito.doAnswer(inv -> {
                destroyCount.incrementAndGet();
                return null;
            }).when(mockChecker).destroy();

            final CachedChecker cachedChecker = expiredCachedChecker(mockChecker);

            // Use a shared ConfigurationLocation mock
            final org.infernus.idea.checkstyle.model.ConfigurationLocation location =
                    mock(org.infernus.idea.checkstyle.model.ConfigurationLocation.class);
            org.mockito.Mockito.when(location.getId()).thenReturn("test");

            cache.put(location, null, cachedChecker);

            final CountDownLatch startLatch = new CountDownLatch(1);
            final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            final List<java.util.concurrent.Future<?>> futures = new ArrayList<>();

            for (int t = 0; t < threadCount; t++) {
                final boolean doInvalidate = (t % 2 == 0);
                futures.add(executor.submit(() -> {
                    try {
                        startLatch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    if (doInvalidate) {
                        cache.invalidate();
                    } else {
                        cache.get(location, null);
                    }
                }));
            }

            startLatch.countDown();
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            for (java.util.concurrent.Future<?> f : futures) {
                f.get();
            }

            assertThat(
                    "Checker must not be destroyed more than once in iteration " + iter,
                    destroyCount.get(),
                    lessThanOrEqualTo(1));
        }
    }

    private CachedChecker expiredCachedChecker(final CheckStyleChecker checker) throws Exception {
        final CachedChecker cachedChecker = new CachedChecker(checker);
        final Field timeStampField = CachedChecker.class.getDeclaredField("timeStamp");
        timeStampField.setAccessible(true);
        timeStampField.set(cachedChecker, System.currentTimeMillis() - 120_000); // expired
        return cachedChecker;
    }

    @Test
    public void cacheIsConsistentAfterConcurrentInvalidateAndGet() throws Exception {
        final CheckerFactoryCache cache = new CheckerFactoryCache();
        final CheckStyleChecker mockChecker = mock(CheckStyleChecker.class);

        final org.infernus.idea.checkstyle.model.ConfigurationLocation location =
                mock(org.infernus.idea.checkstyle.model.ConfigurationLocation.class);
        org.mockito.Mockito.when(location.getId()).thenReturn("test");

        final int threads = 8;
        final CountDownLatch latch = new CountDownLatch(1);
        final ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            final boolean put = (i % 3 == 0);
            executor.submit(() -> {
                try {
                    latch.await();
                    if (put) {
                        cache.put(location, null, new CachedChecker(mockChecker));
                    } else {
                        cache.invalidate();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Whatever state the cache ended up in, it must not throw
        cache.invalidate();
        assertThat(cache.get(location, null).isPresent(), is(false));
    }
}
