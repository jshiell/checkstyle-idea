package org.infernus.idea.checkstyle.checker;

/**
 * What a scan should do with the results already on display.
 */
public enum ResultHandling {

    /**
     * Replace everything on display with the results of this scan.
     */
    REPLACE,

    /**
     * Merge the results of this scan into those already on display, leaving the results for files
     * that were not scanned untouched.
     */
    MERGE

}
