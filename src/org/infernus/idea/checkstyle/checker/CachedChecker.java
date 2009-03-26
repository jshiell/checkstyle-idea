package org.infernus.idea.checkstyle.checker;

import com.puppycrawl.tools.checkstyle.Checker;

/**
 * Key for checker cache.
 */
class CachedChecker {

    /**
     * We cache purely to ignore repeated requests in a multi-file scan. Hence we'll treat the cached
     * value as valid for time in ms.
     */
    private static final int CACHE_VALID_TIME = 60000;

    private Checker checker;
    private long timeStamp;

    /**
     * Create a new checker value.
     *
     * @param checker the checker instance.
     */
    public CachedChecker(final Checker checker) {
        if (checker == null) {
            throw new IllegalArgumentException(
                    "Checker may not be null");
        }

        this.checker = checker;
        this.timeStamp = System.currentTimeMillis();
    }

    /**
     * Get the checker.
     *
     * @return the checker.
     */
    public Checker getChecker() {
        this.timeStamp = System.currentTimeMillis();
        return checker;
    }

    /**
     * Get the timestamp of the config file.
     *
     * @return the timestamp of the config file.
     */
    public long getTimeStamp() {
        return timeStamp;
    }

    public boolean isValid() {
        return (getTimeStamp() + CACHE_VALID_TIME) >= System.currentTimeMillis();
    }
}
