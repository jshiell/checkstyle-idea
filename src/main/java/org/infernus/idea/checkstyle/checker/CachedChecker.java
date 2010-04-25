package org.infernus.idea.checkstyle.checker;

import com.puppycrawl.tools.checkstyle.Checker;
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

    private Checker checker;
    private long timeStamp;
    private Configuration config;

    /**
     * Create a new checker value.
     *
     * @param checker the checker instance.
     * @param config the checker configuration.
     */
    public CachedChecker(final Checker checker, final Configuration config) {
        if (checker == null) {
            throw new IllegalArgumentException(
                    "Checker may not be null");
        }

        this.checker = checker;
        this.timeStamp = System.currentTimeMillis();
        this.config = config;
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
     * Get the config.
     *
     * @return the config.
     */
    public Configuration getConfig() {
        return config;
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
