package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.csapi.SeverityLevel;
import org.jetbrains.annotations.NotNull;

public class ProblemResultTreeInfo extends ResultTreeNode {

    private final PsiFile file;
    private final ResultProblem problem;
    private final SeverityLevel severity;

    /**
     * Construct a node for a given problem.
     *
     * @param file    the file the problem exists in.
     * @param problem the problem.
     */
    public ProblemResultTreeInfo(@NotNull final PsiFile file,
                                 @NotNull final ResultProblem problem) {
        super(CheckStyleBundle.message("plugin.results.file-result",
                file.getName(),
                problem.message(),
                problem.line(),
                Integer.toString(problem.column()),
                problem.sourceCheck()));

        this.file = file;
        this.problem = problem;

        severity = problem.severityLevel();

        updateIconsForProblem();
    }

    private void updateIconsForProblem() {
        if (SeverityLevel.Ignore.equals(severity)) {
            setIcon(AllIcons.General.Note);
        } else if (SeverityLevel.Warning.equals(severity)) {
            setIcon(AllIcons.General.Warning);
        } else if (SeverityLevel.Info.equals(severity)) {
            setIcon(AllIcons.General.Information);
        } else {
            setIcon(AllIcons.General.Error);
        }
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
     * Get the problem associated with this node.
     *
     * @return the problem associated with this node.
     */
    public ResultProblem getProblem() {
        return problem;
    }

    /**
     * Get the severity of the problem.
     *
     * @return the severity, or null if not applicable.
     */
    public SeverityLevel getSeverity() {
        return severity;
    }
}
