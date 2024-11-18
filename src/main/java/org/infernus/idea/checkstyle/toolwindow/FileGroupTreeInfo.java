package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.icons.AllIcons;

public class FileGroupTreeInfo extends GroupTreeInfo {

    /**
     * Construct a file node.
     *
     * @param fileName     the name of the file.
     * @param problemCount the number of problems in the file.
     */
    public FileGroupTreeInfo(final String fileName, final int problemCount) {
        super(fileName, "file", AllIcons.FileTypes.Java, problemCount);
    }

}
