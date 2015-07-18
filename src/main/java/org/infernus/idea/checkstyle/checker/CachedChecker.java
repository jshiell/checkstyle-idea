package org.infernus.idea.checkstyle.checker;

import com.puppycrawl.tools.checkstyle.api.Configuration;

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
    private final Configuration config;

    private long timeStamp;

    /**
     * Create a new checker value.
     *
     * @param checkStyleChecker the checker instance.
     * @param config           the checker configuration.
     */
    public CachedChecker(final CheckStyleChecker checkStyleChecker,
                         final Configuration config) {
        if (checkStyleChecker == null) {
            throw new IllegalArgumentException("Checker may not be null");
        }

        this.checkStyleChecker = checkStyleChecker;
        this.timeStamp = System.currentTimeMillis();
        this.config = config;
    }

    public CheckStyleChecker getCheckStyleChecker() {
        this.timeStamp = System.currentTimeMillis();
        return checkStyleChecker;
    }

    public Configuration getConfig() {
        return config;
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
