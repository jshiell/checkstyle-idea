package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.checker.ConfigurationLocationResult;
import org.infernus.idea.checkstyle.checker.Problem;
import org.infernus.idea.checkstyle.csapi.SeverityLevel;
import org.infernus.idea.checkstyle.util.DisplayFormats;
import org.jetbrains.annotations.NotNull;

record ResultProblem(
        ConfigurationLocationResult configurationLocationResult,
        Module module,
        PsiElement target,
        SeverityLevel severityLevel,
        int line,
        int column,
        String sourceName,
        String message,
        boolean afterEndOfLine,
        boolean suppressErrors) {

    ResultProblem(final ConfigurationLocationResult configurationLocationResult,
                  final Module module,
                  final Problem csProblem) {
        this(configurationLocationResult, module, csProblem.target(), csProblem.severityLevel(), csProblem.line(),
                csProblem.column(), csProblem.sourceName(), csProblem.message(), csProblem.afterEndOfLine(),
                csProblem.suppressErrors());
    }

    @NotNull
    public String sourceCheck() {
        if (sourceName != null) {
            return DisplayFormats.shortenClassName(sourceName);
        }
        return CheckStyleBundle.message("plugin.results.unknown-source");
    }

}
