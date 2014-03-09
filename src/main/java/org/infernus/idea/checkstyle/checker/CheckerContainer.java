package org.infernus.idea.checkstyle.checker;

import com.puppycrawl.tools.checkstyle.Checker;

/**
 * Holds a checker with any extra details we may need.
 */
public class CheckerContainer {
    private final Checker checker;
    private final int tabWidth;

    public CheckerContainer(final Checker checker, final int tabWidth) {
        if (checker == null) {
            throw new IllegalArgumentException("checker may not be null");
        }

        this.checker = checker;
        this.tabWidth = tabWidth;
    }

    public Checker getChecker() {
        return checker;
    }

    public int getTabWidth() {
        return tabWidth;
    }

    public void destroy() {
        checker.destroy();
    }
}
