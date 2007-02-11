package org.infernus.idea.checkstyle.util;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.QuickFix;
import com.intellij.psi.PsiElement;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import org.jetbrains.annotations.NotNull;

/**
 * This is a delegate class used to escape IntelliJ's lack
 * of interest in the severity level or column numbers.
 *
 * @author James Shiell
 * @version 1.0
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

    /**
     * {@inheritDoc}
     */
    public PsiElement getEndElement() {
        return delegate.getEndElement();
    }

    /**
     * {@inheritDoc}
     */
    public ProblemHighlightType getHighlightType() {
        return delegate.getHighlightType();
    }

    /**
     * {@inheritDoc}
     */
    public int getLineNumber() {
        return delegate.getLineNumber();
    }

    /**
     * {@inheritDoc}
     */
    public PsiElement getPsiElement() {
        return delegate.getPsiElement();
    }

    /**
     * {@inheritDoc}
     */
    public PsiElement getStartElement() {
        return delegate.getStartElement();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAfterEndOfLine() {
        return delegate.isAfterEndOfLine();
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    public String getDescriptionTemplate() {
        return delegate.getDescriptionTemplate();
    }

    /**
     * {@inheritDoc}
     */
    public QuickFix[] getFixes() {
        return delegate.getFixes();
    }
}
