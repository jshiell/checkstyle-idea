package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.icons.AllIcons;
import org.infernus.idea.checkstyle.CheckStyleBundle;

public class FileResultTreeInfo extends ResultTreeNode {

    private final String fileName;
    private final int totalProblems;
    private int visibleProblems;

    /**
     * Construct a file node.
     *
     * @param fileName     the name of the file.
     * @param problemCount the number of problems in the file.
     */
    public FileResultTreeInfo(final String fileName, final int problemCount) {
        super(CheckStyleBundle.message("plugin.results.scan-file-result", fileName, problemCount));

        if (fileName == null) {
            throw new IllegalArgumentException("Filename may not be null");
        }

        this.fileName = fileName;
        this.totalProblems = problemCount;
        this.visibleProblems = problemCount;

        updateTextForFileNode();

        setIcon(AllIcons.FileTypes.Java);
    }

    private void updateTextForFileNode() {
        if (totalProblems == visibleProblems) {
            setText(CheckStyleBundle.message("plugin.results.scan-file-result", fileName, totalProblems));
        } else {
            setText(CheckStyleBundle.message("plugin.results.scan-file-result.filtered", fileName, visibleProblems, totalProblems - visibleProblems));
        }
    }

    void setVisibleProblems(final int visibleProblems) {
        this.visibleProblems = visibleProblems;

        updateTextForFileNode();
    }
}
