package org.infernus.idea.checkstyle.checker;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.QuickFix;
import com.intellij.lang.annotation.ProblemGroup;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This is a delegate class used to escape IntelliJ's lack
 * of interest in the severity level or column numbers.
 */
public class ExtendedProblemDescriptor implements ProblemDescriptor {

    private final ProblemDescriptor delegate;
    private final SeverityLevel severity;
    private final int column;
    private final int line;

    public ExtendedProblemDescriptor(final ProblemDescriptor delegate,
                                     final SeverityLevel severity,
                                     final int line,
                                     final int column) {
        if (delegate == null) {
            throw new IllegalArgumentException("Delegate may not be null.");
        }
        if (severity == null) {
            throw new IllegalArgumentException("Severity may not be null.");
        }

        this.delegate = delegate;
        this.severity = severity;
        this.line = line;
        this.column = column;
    }

    /**
     * Get the CheckStyle severity of this problem.
     *
     * @return the CheckStyle severity of this problem.
     */
    public SeverityLevel getSeverity() {
        return severity;
    }

    /**
     * Get the column position of this error.
     *
     * @return the column position.
     */
    public int getColumn() {
        return column;
    }

    /**
     * Get the line position of this error.
     * <p/>
     * This is the line as reported by CheckStyle, rather than that computed
     * by IDEA.
     *
     * @return the line position.
     */
    public int getLine() {
        return line;
    }

    public PsiElement getEndElement() {
        return delegate.getEndElement();
    }

    @NotNull
    public ProblemHighlightType getHighlightType() {
        return delegate.getHighlightType();
    }

    public int getLineNumber() {
        return delegate.getLineNumber();
    }

    public PsiElement getPsiElement() {
        return delegate.getPsiElement();
    }

    public PsiElement getStartElement() {
        return delegate.getStartElement();
    }

    public boolean isAfterEndOfLine() {
        return delegate.isAfterEndOfLine();
    }

    @NotNull
    public String getDescriptionTemplate() {
        return delegate.getDescriptionTemplate();
    }

    public QuickFix[] getFixes() {
        return delegate.getFixes();
    }

    public void setTextAttributes(final TextAttributesKey textAttributesKey) {
        delegate.setTextAttributes(textAttributesKey);
    }

    public boolean showTooltip() {
        return delegate.showTooltip();
    }

    @Nullable
    public ProblemGroup getProblemGroup() {
        return delegate.getProblemGroup();
    }

    public void setProblemGroup(@Nullable final ProblemGroup problemGroup) {
        delegate.setProblemGroup(problemGroup);
    }

    @Override
    public TextRange getTextRangeInElement() {
        return delegate.getTextRangeInElement();
    }
}
