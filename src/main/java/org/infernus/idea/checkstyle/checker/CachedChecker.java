package org.infernus.idea.checkstyle.checker;

import org.jetbrains.annotations.NotNull;

/**
 * Key for checker cache.
 */
class CachedChecker {

    /**
     * We cache purely to ignore repeated requests in a multi-file scan. Hence we'll treat the cached
     * value as valid for time in ms.
     */
    private static final int CACHE_VALID_TIME = 60000;

    private final CheckStyleChecker checkStyleChecker;

    private long timeStamp;

    public CachedChecker(@NotNull final CheckStyleChecker checkStyleChecker) {
        this.checkStyleChecker = checkStyleChecker;
        this.timeStamp = System.currentTimeMillis();
    }

    public CheckStyleChecker getCheckStyleChecker() {
        this.timeStamp = System.currentTimeMillis();
        return checkStyleChecker;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public boolean isValid() {
        return (getTimeStamp() + CACHE_VALID_TIME) >= System.currentTimeMillis();
    }

    public void destroy() {
        checkStyleChecker.destroy();
    }

}
