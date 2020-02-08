package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Value for checker cache.
 */
class CachedChecker {

    /**
     * We cache purely to ignore repeated requests in a multi-file scan. Hence we'll treat the cached
     * value as valid for time in ms.
     */
    private static final int CACHE_VALID_TIME = 60000;

    private final Project project;

    private final CheckStyleChecker checkStyleChecker;

    private long timeStamp;

    CachedChecker(@NotNull final Project project, @NotNull final CheckStyleChecker checkStyleChecker) {
        this.project = project;
        this.checkStyleChecker = checkStyleChecker;
        this.timeStamp = System.currentTimeMillis();
    }

    public CheckStyleChecker getCheckStyleChecker() {
        this.timeStamp = System.currentTimeMillis();
        return checkStyleChecker;
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    private long getTimeStamp() {
        return timeStamp;
    }

    public boolean isValid() {
        return (getTimeStamp() + CACHE_VALID_TIME) >= System.currentTimeMillis();
    }

    public void destroy() {
        checkStyleChecker.destroy();
    }
}
