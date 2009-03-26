package org.infernus.idea.checkstyle.checker;

import com.puppycrawl.tools.checkstyle.Checker;

/**
 * Key for checker cache.
 */
class CachedChecker {

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

    public void updateTimeStamp() {
        this.timeStamp = System.currentTimeMillis();
    }
}
