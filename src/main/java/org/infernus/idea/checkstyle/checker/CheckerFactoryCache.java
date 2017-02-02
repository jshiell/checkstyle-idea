package org.infernus.idea.checkstyle.checker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.intellij.concurrency.JobScheduler;
import com.intellij.openapi.module.Module;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CheckerFactoryCache
{
    private static final int CLEANUP_PERIOD_SECONDS = 30;

    // TODO This may work more reliably if we just used a ConcurrentHashMap instead of our own reimplementation.
    // TODO We should check for expiration only when we return a Checker from the cache, so that we don't need the
    //      backgroundCleanupTask.
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private final Map<CheckerFactoryCacheKey, CachedChecker> cache = new HashMap<>();

    private final ScheduledExecutorService cleanUpExecutor = JobScheduler.getScheduler();

    public CheckerFactoryCache() {
        startBackgroundCleanupTask();
    }

    // TODO Why would a cache return Optionals? The value should either be present or not present.
    public Optional<CachedChecker> get(@NotNull final ConfigurationLocation location, @Nullable final Module module) {
        final CheckerFactoryCacheKey key = new CheckerFactoryCacheKey(location, module);
        cacheLock.readLock().lock();
        try {
            if (cache.containsKey(key)) {
                final CachedChecker cachedChecker = cache.get(key);
                if (cachedChecker != null && cachedChecker.isValid()) {
                    return Optional.of(cachedChecker);
                } else {
                    cacheLock.readLock().unlock();
                    writeToCache(() -> {
                        try {
                            if (cachedChecker != null) {
                                cachedChecker.destroy();
                            }
                        } finally {
                            return cache.remove(key);
                        }
                    });
                    cacheLock.readLock().lock();
                }
            }
            return Optional.empty();
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    public void put(@NotNull final ConfigurationLocation location, final Module module, @NotNull final CachedChecker
            checker) {
        writeToCache(() -> cache.put(new CheckerFactoryCacheKey(location, module),
                checker));
    }

    public void invalidate() {
        writeToCache(() -> {
            try {
                cache.values().forEach(this::destroyChecker);
            } finally {
                cache.clear();
            }
            return null;
        });
    }

    private void destroyChecker(@NotNull final CachedChecker pCachedChecker) {
        pCachedChecker.destroy();
    }

    private void startBackgroundCleanupTask() {
        // Use {@link ScheduleThreadPoolExecutor#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)} rather
        // than {@link ScheduleThreadPoolExecutor#scheduleWithFixedRate(Runnable, long, long, TimeUnit)} as
        // recommended by JetBrains for compatibility with hibernation.
        cleanUpExecutor.scheduleWithFixedDelay(this::cleanUpExpiredCachedCheckers, CLEANUP_PERIOD_SECONDS,
                CLEANUP_PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    private void cleanUpExpiredCachedCheckers() {
        writeToCache(() -> {
            final List<CheckerFactoryCacheKey> itemsToRemove = cache.entrySet().stream().filter(cacheEntry ->
                    cacheEntry.getValue() != null && !cacheEntry.getValue().isValid()).map(cacheEntry -> {
                destroyChecker(cacheEntry.getValue());
                return cacheEntry.getKey();
            }).collect(Collectors.toList());
            return cache.entrySet().removeIf(entry -> itemsToRemove.contains(entry.getKey()));
        });
    }

    private <T> T writeToCache(final Supplier<T> task) {
        cacheLock.writeLock().lock();
        try {
            return task.get();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
}
