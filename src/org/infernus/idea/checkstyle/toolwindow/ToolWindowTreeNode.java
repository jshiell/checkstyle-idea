package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.psi.PsiFile;

import javax.swing.*;

/**
 * The user object for meta-data on tree nodes in the tool window.
 *
 * @author James Shiell
 * @version 1.0
 */
public class ToolWindowTreeNode {

    private PsiFile file;
    private ProblemDescriptor problem;
    private Icon expandedIcon;
    private Icon collapsedIcon;
    private String text;
    private String tooltip;
    private String description;

    /**
     * Construct a node with the given display text.
     *
     * @param text the text of the node.
     */
    public ToolWindowTreeNode(final String text) {
        this.text = text;
    }

    /**
     * Construct a node for a given problem.
     *
     * @param file the file the problem exists in.
     * @param problem the problem.
     */
    public ToolWindowTreeNode(final PsiFile file, final ProblemDescriptor problem) {
        if (file == null) {
            throw new IllegalArgumentException("File may not be null");
        }
        if (problem == null) {
            throw new IllegalArgumentException("Problem may not be null");
        }

        this.file = file;
        this.problem = problem;
    }

    /**
     * Get the problem associated with this node.
     *
     * @return the problem associated with this node.
     */
    public ProblemDescriptor getProblem() {
        return problem;
    }
    
    /**
     * Get the node's icon when in an expanded state.
     *
     * @return the node's icon when in an expanded state.
     */
    public Icon getExpandedIcon() {
        return expandedIcon;
    }

    /**
     * Set the node's icon when in an expanded state.
     *
     * @param expandedIcon the node's icon when in an expanded state.
     */
    public void setExpandedIcon(final Icon expandedIcon) {
        this.expandedIcon = expandedIcon;
    }

    /**
     * Get the node's icon when in an collapsed state.
     *
     * @return the node's icon when in an collapsed state.
     */
    public Icon getCollapsedIcon() {
        return collapsedIcon;
    }

    /**
     * Set the node's icon when in an collapsed state.
     *
     * @param collapsedIcon the node's icon when in an collapsed state.
     */
    public void setCollapsedIcon(Icon collapsedIcon) {
        this.collapsedIcon = collapsedIcon;
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
    public void setText(final String text) {
        if (text == null || text.trim().length() == 0) {
            throw new IllegalArgumentException("Text may not be null/empty");
        }
        this.text = text;
    }

    /**
     * Get the file associated with this node.
     *
     * @return the file associated with this node.
     */
    public PsiFile getFile() {
        return file;
    }

    /**
     * Get the tooltip for this node, if any.
     *
     * @return the tooltip for this node, or null if none.
     */
    public String getTooltip() {
        return tooltip;
    }

    /**
     * Set the tooltip for this node, if any.
     *
     * @param tooltip the tooltip for this node, or null if none.
     */
    public void setTooltip(final String tooltip) {
        this.tooltip = tooltip;
    }

    /**
     * Get the description of this node, if any.
     *
     * @return the description of this node, or null if none.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the description of this node, if any.
     *
     * @param description the description of this node, or null if none.
     */
    public void setDescription(final String description) {
        this.description = description;
    }
}
