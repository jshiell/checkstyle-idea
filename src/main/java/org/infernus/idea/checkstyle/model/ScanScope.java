package org.infernus.idea.checkstyle.model;

import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.jetbrains.annotations.NotNull;

/**
 * Possible values of the 'scope' configuration item.
 */
public enum ScanScope {
    /**
     * Scan only Java files which reside in source folders (main only)
     */
    JavaOnly,

    /**
     * Scan only Java files which reside in source folders (main <i>and</i> test)
     */
    JavaOnlyWithTests,

    /**
     * Scan all files which reside in source folders (main only)
     */
    AllSources,

    /**
     * Scan all files which reside in source folders (main <i>and</i> test)
     */
    AllSourcesWithTests,

    /**
     * Scan <i>all</i> files in the project, regardless of their location
     */
    Everything;

    private final String dropdownBoxEntry;

    ScanScope() {
        dropdownBoxEntry = CheckStyleBundle.message("config.scanscope." + this.name());
    }

    public boolean includeTestClasses() {
        return this == JavaOnlyWithTests || this == AllSourcesWithTests || this == Everything;
    }

    public boolean includeNonJavaSources() {
        return this == AllSources || this == AllSourcesWithTests || this == Everything;
    }

    @NotNull
    public static ScanScope fromFlags(final boolean pIncludeTests, final boolean pIncludeNonJava) {
        ScanScope result = getDefaultValue();
        if (pIncludeTests) {
            if (pIncludeNonJava) {
                result = AllSourcesWithTests;
            } else {
                result = JavaOnlyWithTests;
            }
        } else if (pIncludeNonJava) {
            result = AllSources;
        }
        return result;
    }

    @NotNull
    public static ScanScope getDefaultValue() {
        return JavaOnly;
    }

    @Override
    public String toString() {
        return dropdownBoxEntry;
    }
}
