package org.infernus.idea.checkstyle.checker;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.psi.PsiElement;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.csapi.SeverityLevel;
import org.infernus.idea.checkstyle.util.DisplayFormats;
import org.infernus.idea.checkstyle.util.Objects;
import org.jetbrains.annotations.NotNull;

public record Problem(@NotNull PsiElement target,
                      @NotNull String message,
                      @NotNull SeverityLevel severityLevel,
                      int line,
                      int column,
                      String sourceName,
                      boolean afterEndOfLine,
                      boolean suppressErrors) implements Comparable<Problem> {

    @NotNull
    public ProblemDescriptor toProblemDescriptor(final InspectionManager inspectionManager,
                                                 final boolean onTheFly) {
        return inspectionManager.createProblemDescriptor(target,
                CheckStyleBundle.message("inspection.message", message()),
                quickFixes(), problemHighlightType(), onTheFly, afterEndOfLine);
    }

    private LocalQuickFix[] quickFixes() {
        if (sourceName != null) {
            return new LocalQuickFix[]{new SuppressForCheckstyleFix(DisplayFormats.shortenClassName(sourceName))};
        }
        return null;
    }

    private ProblemHighlightType problemHighlightType() {
        if (!suppressErrors) {
            return switch (severityLevel()) {
                case Error -> ProblemHighlightType.GENERIC_ERROR;
                case Info -> ProblemHighlightType.WEAK_WARNING;
                case Ignore -> ProblemHighlightType.INFORMATION;
                default -> ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
            };
        }
        return ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
    }

    @Override
    public int compareTo(@NotNull final Problem other) {
        int lineComparison = Integer.compare(this.line, other.line);
        if (lineComparison == 0) {
            int columnComparison = Integer.compare(this.column, other.column);
            if (columnComparison == 0) {
                int severityComparison = -this.severityLevel.compareTo(other.severityLevel);
                if (severityComparison == 0) {
                    return Objects.compare(this.message, other.message);
                }
                return severityComparison;
            }
            return columnComparison;
        }
        return lineComparison;
    }

}
