package org.infernus.idea.checkstyle.checker;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.psi.PsiElement;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.csapi.SeverityLevel;
import org.jetbrains.annotations.NotNull;

public class Problem
{
    private final PsiElement target;
    private final SeverityLevel severityLevel;
    private final int line;
    private final int column;
    private final String message;
    private final boolean afterEndOfLine;
    private final boolean suppressErrors;

    public Problem(@NotNull final PsiElement target, @NotNull final String pMessage, @NotNull final SeverityLevel
            pSeverityLevel, final int pLine, final int pColumn, final boolean afterEndOfLine, final boolean
            suppressErrors) {

        this.target = target;
        this.message = pMessage;
        this.severityLevel = pSeverityLevel;
        this.line = pLine;
        this.column = pColumn;
        this.afterEndOfLine = afterEndOfLine;
        this.suppressErrors = suppressErrors;
    }

    @NotNull
    public ProblemDescriptor toProblemDescriptor(final InspectionManager inspectionManager) {
        return inspectionManager.createProblemDescriptor(target, CheckStyleBundle.message("inspection.message",
                message()), null, problemHighlightType(), false, afterEndOfLine);
    }

    @NotNull
    public String message() {
        return message;
    }

    @NotNull
    public SeverityLevel severityLevel() {
        return severityLevel;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }

    private ProblemHighlightType problemHighlightType() {
        if (!suppressErrors) {
            switch (severityLevel()) {
                case Error:
                    return ProblemHighlightType.ERROR;
                case Warning:
                    return ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
                case Info:
                    return ProblemHighlightType.WEAK_WARNING;
                case Ignore:
                    return ProblemHighlightType.INFORMATION;
                default:
                    return ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
            }
        }
        return ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
    }
}
