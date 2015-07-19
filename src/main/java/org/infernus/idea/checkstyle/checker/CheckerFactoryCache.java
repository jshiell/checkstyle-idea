package org.infernus.idea.checkstyle.checker;

import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CheckerFactoryCache {
    private static final int ONE_THREAD = 1;
    private static final int CLEANUP_PERIOD_SECONDS = 30;

    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private final Map<ConfigurationLocation, CachedChecker> cache = new HashMap<>();

    private final ScheduledExecutorService cleanUpExecutor = Executors.newScheduledThreadPool(ONE_THREAD);

    public CheckerFactoryCache() {
        startBackgroundCleanupTask();
    }

    public void shutdown() {
        cleanUpExecutor.shutdown();
    }

    public Optional<CachedChecker> get(@NotNull final ConfigurationLocation location) {
        cacheLock.readLock().lock();
        try {
            if (cache.containsKey(location)) {
                final CachedChecker cachedChecker = cache.get(location);
                if (cachedChecker != null && cachedChecker.isValid()) {
                    return Optional.of(cachedChecker);

                } else {
                    cacheLock.readLock().unlock();
                    writeToCache(() -> {
                        if (cachedChecker != null) {
                            cachedChecker.destroy();
                        }
                        return cache.remove(location);
                    });
                    cacheLock.readLock().lock();
                }
            }
            return Optional.empty();

        } finally {
            cacheLock.readLock().unlock();
        }
    }

    public void put(@NotNull final ConfigurationLocation location, @NotNull final CachedChecker checker) {
        writeToCache(() -> cache.put(location, checker));
    }

    public void invalidate() {
        writeToCache(() -> {
            cache.values().forEach(CachedChecker::destroy);
            cache.clear();
            return null;
        });
    }

    private void startBackgroundCleanupTask() {
        cleanUpExecutor.scheduleAtFixedRate(this::cleanUpExpiredCachedCheckers,
                CLEANUP_PERIOD_SECONDS, CLEANUP_PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    private void cleanUpExpiredCachedCheckers() {
        writeToCache(() -> {
            final List<ConfigurationLocation> itemsToRemove = cache.entrySet().stream()
                    .filter(cacheEntry -> cacheEntry.getValue() != null && !cacheEntry.getValue().isValid())
                    .map(cacheEntry -> {
                        cacheEntry.getValue().destroy();
                        return cacheEntry.getKey();
                    })
                    .collect(Collectors.toList());
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
