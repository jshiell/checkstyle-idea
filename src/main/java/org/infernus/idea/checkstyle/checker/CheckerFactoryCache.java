package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CheckerFactoryCache {

    private static final Logger LOG = Logger.getInstance(CheckerFactoryCache.class);

    private final Map<CheckerFactoryCacheKey, CachedChecker> cache = new ConcurrentHashMap<>();

    public Optional<CachedChecker> get(@NotNull final ConfigurationLocation location,
                                       @Nullable final Module module) {
        cleanUpExpiredCachedCheckers();

        final CheckerFactoryCacheKey key = new CheckerFactoryCacheKey(location, module);

        final CachedChecker cachedChecker = cache.get(key);
        if (cachedChecker != null && cachedChecker.isValid()) {
            return Optional.of(cachedChecker);
        }

        if (cachedChecker != null) {
            cache.remove(key);
            destroyChecker(cachedChecker);
        }
        return Optional.empty();
    }

    public void put(@NotNull final ConfigurationLocation location,
                    @Nullable final Module module,
                    @NotNull final CachedChecker checker) {
        cache.put(new CheckerFactoryCacheKey(location, module), checker);
    }

    public void invalidate() {
        List<CachedChecker> existingCheckers = new ArrayList<>(cache.values());
        cache.clear();

        existingCheckers.forEach(this::destroyChecker);
    }

    private void destroyChecker(final CachedChecker cachedChecker) {
        try {
            if (cachedChecker != null) {
                cachedChecker.destroy();
            }
        } catch (Exception ignored) {
        }
    }

    private void cleanUpExpiredCachedCheckers() {
        try {
            List<CachedChecker> checkersToDestroy = new ArrayList<>();

            for (Iterator<Map.Entry<CheckerFactoryCacheKey, CachedChecker>> i = cache.entrySet().iterator(); i.hasNext();) {
                Map.Entry<CheckerFactoryCacheKey, CachedChecker> cacheEntry = i.next();
                if (cacheEntry.getValue() == null || !cacheEntry.getValue().isValid()) {
                    checkersToDestroy.add(cacheEntry.getValue());
                    i.remove();
                }
            }

            checkersToDestroy.forEach(this::destroyChecker);

        } catch (Exception e) {
            LOG.error("Cleanup failed", e);
        }
    }
}
