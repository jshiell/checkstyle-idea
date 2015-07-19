package org.infernus.idea.checkstyle.checker;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.psi.PsiFile;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import org.infernus.idea.checkstyle.checks.Check;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class CheckStyleExtendedAuditListener extends CheckStyleAuditListener {

    public CheckStyleExtendedAuditListener(@NotNull final Map<String, PsiFile> fileNamesToPsiFiles,
                                           @NotNull final InspectionManager manager,
                                           final boolean suppressErrors,
                                           final int tabWidth,
                                           @NotNull final List<Check> checks) {
        super(fileNamesToPsiFiles, manager, suppressErrors, tabWidth, checks);
    }

    @Override
    protected ProblemDescriptor annotateProblem(@NotNull final AuditEvent event,
                                                @NotNull final ProblemDescriptor problem) {
        return extendDescriptor(event, problem);
    }

    private ProblemDescriptor extendDescriptor(final AuditEvent event,
                                               final ProblemDescriptor problem) {
        return new ExtendedProblemDescriptor(problem, event.getSeverityLevel(),
                event.getLine(), event.getColumn());
    }

}
