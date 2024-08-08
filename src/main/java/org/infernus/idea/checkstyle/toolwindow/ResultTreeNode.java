package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.icons.AllIcons;

import javax.swing.*;

import static org.infernus.idea.checkstyle.util.Strings.isBlank;


/**
 * The user object for meta-data on tree nodes in the tool window.
 */
public class ResultTreeNode {

    private String text;
    private Icon icon = AllIcons.General.Information;

    /**
     * Construct an informational node.
     *
     * @param text the information text.
     */
    public ResultTreeNode(final String text) {
        if (text == null) {
            throw new IllegalArgumentException("Text may not be null");
        }

        this.text = text;
    }

    /**
     * Get the node's icon when in an expanded state.
     *
     * @return the node's icon when in an expanded state.
     */
    public Icon getExpandedIcon() {
        return icon;
    }

    /**
     * Get the node's icon when in a collapsed state.
     *
     * @return the node's icon when in a collapsed state.
     */
    public Icon getCollapsedIcon() {
        return icon;
    }

    /**
     * Get the file the node represents.
     *
     * @return the file the node represents.
     */
    public String getText() {
        return text;
    }

    /**
     * Set the file the node represents.
     *
     * @param text the file the node represents.
     */
    void setText(final String text) {
        if (isBlank(text)) {
            throw new IllegalArgumentException("Text may not be null/empty");
        }
        this.text = text;
    }

    protected void setIcon(final Icon icon) {
        this.icon = icon;
    }

    @Override
    public String toString() {
        return text;
    }
}
