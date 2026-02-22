package org.infernus.idea.checkstyle.toolwindow;

import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

abstract class GroupTreeInfo extends ResultTreeNode {

    private final String name;
    private final String groupId;
    private final int totalProblems;
    private int visibleProblems;

    /**
     * Construct a group node.
     *
     * @param name         the name of the group.
     * @param groupId      the ID used as part of message lookup.
     * @param icon         the icon of the group.
     * @param problemCount the number of problems in the group.
     */
    GroupTreeInfo(@NotNull final String name,
                  @NotNull final String groupId,
                  @NotNull final Icon icon,
                  final int problemCount) {
        super(CheckStyleBundle.message("plugin.results.scan-" + groupId + "-result", name, problemCount));

        this.name = name;
        this.groupId = groupId;
        this.totalProblems = problemCount;
        this.visibleProblems = problemCount;

        updateDisplayText();
        setIcon(icon);
    }

    private void updateDisplayText() {
        if (totalProblems == visibleProblems) {
            setText(CheckStyleBundle.message("plugin.results.scan-" + groupId + "-result", name, totalProblems));
        } else {
            setText(CheckStyleBundle.message("plugin.results.scan-" + groupId + "-result.filtered", name, visibleProblems, totalProblems - visibleProblems));
        }
    }

    void setVisibleProblems(final int visibleProblems) {
        this.visibleProblems = visibleProblems;

        updateDisplayText();
    }
}
