package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.psi.PsiFile;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.util.ExtendedProblemDescriptor;
import org.infernus.idea.checkstyle.util.IDEAUtilities;

import javax.swing.*;

/**
 * The user object for meta-data on tree nodes in the tool window.
 */
public class ResultTreeNode {

    private PsiFile file;
    private ProblemDescriptor problem;
    private Icon icon;
    private String text;
    private String tooltip;
    private String description;
    private SeverityLevel severity;

    /**
     * Construct a informational node.
     *
     * @param text the information text.
     */
    public ResultTreeNode(final String text) {
        if (text == null) {
            throw new IllegalArgumentException("Text may not be null");
        }

        this.text = text;
        icon = IDEAUtilities.getIcon("/general/information.png");
    }


    /**
     * Construct a file node.
     *
     * @param fileName     the name of the file.
     * @param problemCount the number of problems in the file.
     */
    public ResultTreeNode(final String fileName, final int problemCount) {
        if (fileName == null) {
            throw new IllegalArgumentException("Filename may not be null");
        }

        this.text = CheckStyleBundle.message("plugin.results.scan-file-result", fileName, problemCount);
        icon = IDEAUtilities.getIcon("/fileTypes/java.png");
    }

    /**
     * Construct a node for a given problem.
     *
     * @param file    the file the problem exists in.
     * @param problem the problem.
     */
    public ResultTreeNode(final PsiFile file, final ProblemDescriptor problem) {
        if (file == null) {
            throw new IllegalArgumentException("File may not be null");
        }
        if (problem == null) {
            throw new IllegalArgumentException("Problem may not be null");
        }

        this.file = file;
        this.problem = problem;

        if (problem instanceof ExtendedProblemDescriptor) {
            severity = ((ExtendedProblemDescriptor) problem).getSeverity();
        }

        updateIconsForProblem();
    }

    private void updateIconsForProblem() {
        if (severity != null && SeverityLevel.IGNORE.equals(severity)) {
            icon = IDEAUtilities.getIcon("/general/hideWarnings.png");
        } else if (severity != null && SeverityLevel.WARNING.equals(severity)) {
            icon = IDEAUtilities.getIcon("/general/warning.png");
        } else if (severity != null && SeverityLevel.INFO.equals(severity)) {
            icon = IDEAUtilities.getIcon("/general/information.png");
        } else {
            icon = IDEAUtilities.getIcon("/general/error.png");
        }
    }

    /**
     * Get the severity of the problem.
     *
     * @return the severity, or null if not applicable.
     */
    public SeverityLevel getSeverity() {
        return severity;
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
        return icon;
    }

    /**
     * Get the node's icon when in an collapsed state.
     *
     * @return the node's icon when in an collapsed state.
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

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(final Icon icon) {
        this.icon = icon;
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

    public String toString() {
        if (text != null) {
            return text;
        }

        final String column = problem instanceof ExtendedProblemDescriptor
                ? Integer.toString(((ExtendedProblemDescriptor) problem).getColumn()) : "?";
        final int line = problem instanceof ExtendedProblemDescriptor
                ? ((ExtendedProblemDescriptor) problem).getLine()
                : problem.getLineNumber();

        return CheckStyleBundle.message("plugin.results.file-result", file.getName(),
                problem.getDescriptionTemplate(), line, column);
    }
}
