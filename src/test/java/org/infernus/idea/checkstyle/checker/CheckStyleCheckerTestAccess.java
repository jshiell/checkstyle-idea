package org.infernus.idea.checkstyle.checker;

import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.jetbrains.annotations.NotNull;

/**
 * Test-only bridge that exposes the package-private {@link CheckStyleChecker#checkerWithConfig()} accessor
 * to tests outside the {@code checker} package.
 */
public final class CheckStyleCheckerTestAccess {

    private CheckStyleCheckerTestAccess() {
    }

    @NotNull
    public static CheckstyleInternalObject checkerWithConfig(@NotNull final CheckStyleChecker checker) {
        return checker.checkerWithConfig();
    }
}
