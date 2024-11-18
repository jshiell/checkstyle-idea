package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.icons.AllIcons;

public class PackageGroupTreeInfo extends GroupTreeInfo {

    /**
     * Construct a package node.
     *
     * @param packageName  the name of the package.
     * @param problemCount the number of problems in the file.
     */
    public PackageGroupTreeInfo(final String packageName, final int problemCount) {
        super(packageName, "package", AllIcons.Nodes.Package, problemCount);
    }

}
