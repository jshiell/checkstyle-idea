package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CheckerFactoryCache implements Disposable {

    private static final Logger LOG = Logger.getInstance(CheckerFactoryCache.class);

    private final Map<CheckerFactoryCacheKey, CachedChecker> cache = new ConcurrentHashMap<>();

    public Optional<CachedChecker> get(@NotNull final ConfigurationLocation location,
                                       @Nullable final Module module) {
        cleanUpExpiredCachedCheckers();

        final CheckerFactoryCacheKey key = new CheckerFactoryCacheKey(location, module);

        final CachedChecker cachedChecker = cache.get(key);
        if (cachedChecker != null && cachedChecker.isValid()) {
            LOG.debug("Valid cached checker found; returning for ", location);
            return Optional.of(cachedChecker);
        }

        if (cachedChecker != null) {
            LOG.debug("Invalid cached checker found; deleting and returning empty for ", location);
            cache.remove(key);
            destroyChecker(cachedChecker);
        }
        return Optional.empty();
    }

    @Override
    public void dispose() {
        invalidate();
    }

    public void put(@NotNull final ConfigurationLocation location,
                    @Nullable final Module module,
                    @NotNull final CachedChecker checker) {
        cache.put(new CheckerFactoryCacheKey(location, module), checker);
    }

    public void invalidate() {
        LOG.debug("Cache invalidation requested");

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
