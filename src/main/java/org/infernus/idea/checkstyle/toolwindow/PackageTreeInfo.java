package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.icons.AllIcons;
import org.infernus.idea.checkstyle.CheckStyleBundle;

public class PackageTreeInfo extends ResultTreeNode {

    private final String packageName;
    private final int totalProblems;
    private int visibleProblems;

    /**
     * Construct a package node.
     *
     * @param packageName     the name of the package.
     * @param problemCount the number of problems in the file.
     */
    public PackageTreeInfo(final String packageName, final int problemCount) {
        super(CheckStyleBundle.message("plugin.results.scan-file-result", packageName, problemCount));

        if (packageName == null) {
            throw new IllegalArgumentException("Package name may not be null");
        }

        this.packageName = packageName;
        this.totalProblems = problemCount;
        this.visibleProblems = problemCount;

        updateDisplayText();

        setIcon(AllIcons.Nodes.Package);
    }

    private void updateDisplayText() {
        if (totalProblems == visibleProblems) {
            setText(CheckStyleBundle.message("plugin.results.scan-package-result", packageName, totalProblems));
        } else {
            setText(CheckStyleBundle.message("plugin.results.scan-package-result.filtered", packageName, visibleProblems, totalProblems - visibleProblems));
        }
    }

    void setVisibleProblems(final int visibleProblems) {
        this.visibleProblems = visibleProblems;

        updateDisplayText();
    }
}
