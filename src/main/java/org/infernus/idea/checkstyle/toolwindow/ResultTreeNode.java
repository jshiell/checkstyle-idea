package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.psi.PsiFile;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.checker.Problem;
import org.infernus.idea.checkstyle.util.Icons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * The user object for meta-data on tree nodes in the tool window.
 */
public class ResultTreeNode {

    private PsiFile file;
    private Problem problem;
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
        icon = Icons.icon("/general/information.png");
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
        icon = Icons.icon("/fileTypes/java.png");
    }

    /**
     * Construct a node for a given problem.
     *
     * @param file    the file the problem exists in.
     * @param problem the problem.
     */
    public ResultTreeNode(@NotNull final PsiFile file,
                          @NotNull final Problem problem) {
        this.file = file;
        this.problem = problem;

        severity = problem.severityLevel();

        updateIconsForProblem();
    }

    private void updateIconsForProblem() {
        if (severity != null && SeverityLevel.IGNORE.equals(severity)) {
            icon = Icons.icon("/general/hideWarnings.png");
        } else if (severity != null && SeverityLevel.WARNING.equals(severity)) {
            icon = Icons.icon("/general/warning.png");
        } else if (severity != null && SeverityLevel.INFO.equals(severity)) {
            icon = Icons.icon("/general/information.png");
        } else {
            icon = Icons.icon("/general/error.png");
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
    public Problem getProblem() {
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

        return CheckStyleBundle.message("plugin.results.file-result", file.getName(),
                problem.message(), problem.line(), Integer.toString(problem.column()));
    }
}
