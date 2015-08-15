package org.infernus.idea.checkstyle.checker;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.psi.PsiElement;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.jetbrains.annotations.NotNull;

public class Problem {

    private final PsiElement target;
    private final AuditEvent auditEvent;
    private final boolean afterEndOfLine;
    private final boolean suppressErrors;

    public Problem(@NotNull final PsiElement target,
                   @NotNull final AuditEvent auditEvent,
                   final boolean afterEndOfLine,
                   final boolean suppressErrors) {
        this.target = target;
        this.auditEvent = auditEvent;
        this.afterEndOfLine = afterEndOfLine;
        this.suppressErrors = suppressErrors;
    }

    @NotNull
    public ProblemDescriptor toProblemDescriptor(final InspectionManager inspectionManager) {
        return inspectionManager.createProblemDescriptor(
                target,
                CheckStyleBundle.message("inspection.message", message()),
                null,
                problemHighlightType(),
                false,
                afterEndOfLine);
    }

    @NotNull
    public String message() {
        if (auditEvent.getLocalizedMessage() != null) {
            return auditEvent.getLocalizedMessage().getMessage();
        }
        return auditEvent.getMessage();
    }

    @NotNull
    public SeverityLevel severityLevel() {
        return auditEvent.getSeverityLevel();
    }

    public int line() {
        return auditEvent.getLine();
    }

    public int column() {
        return auditEvent.getColumn();
    }

    private ProblemHighlightType problemHighlightType() {
        if (!suppressErrors) {
            switch (severityLevel()) {
                case ERROR:
                    return ProblemHighlightType.ERROR;
                case WARNING:
                    return ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
                case INFO:
                    return ProblemHighlightType.WEAK_WARNING;
                case IGNORE:
                    return ProblemHighlightType.INFORMATION;
                default:
                    return ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
            }
        }
        return ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
    }

}
