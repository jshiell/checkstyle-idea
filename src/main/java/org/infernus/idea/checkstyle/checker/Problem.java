package org.infernus.idea.checkstyle.checker;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;

import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.csapi.SeverityLevel;
import org.infernus.idea.checkstyle.util.DisplayFormats;
import org.infernus.idea.checkstyle.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record Problem(@NotNull PsiElement target,
                      @NotNull String message,
                      @NotNull SeverityLevel severityLevel,
                      int line,
                      int column,
                      String sourceName,
                      boolean afterEndOfLine,
                      boolean suppressErrors) implements Comparable<Problem> {

    /** Extension point name for registering Checkstyle quick fix providers. */
    @SuppressWarnings("UnresolvedPluginConfigReference")
    public static final ExtensionPointName<CheckstyleQuickFixProvider> QUICK_FIX_PROVIDER_EP =
        ExtensionPointName.create("CheckStyle-IDEA.checkstyleQuickFixProvider");

    @NotNull
    public ProblemDescriptor toProblemDescriptor(final InspectionManager inspectionManager,
                                                 final boolean onTheFly) {
        final String sourceCheck = DisplayFormats.shortenClassName(sourceName);
        return inspectionManager.createProblemDescriptor(target,
                CheckStyleBundle.message("inspection.message", message, sourceCheck),
                quickFixes(sourceCheck), problemHighlightType(), onTheFly, afterEndOfLine);
    }

    /**
     * Retrieves quick fixes for the current problem by combining suppress fixes and extension-provided fixes.
     * <p>
     * First, if {@code sourceCheck} is not null, a {@link SuppressForCheckstyleFix} is added to the fixes list.
     * Then, it queries the {@code CheckstyleQuickFixProvider} extension point for additional fixes from registered providers.
     * If no fixes are found, returns {@code null}; otherwise, returns an array of all collected fixes.
     *
     * @param sourceCheck The source check name used to create a suppress fix, or {@code null} if not applicable
     * @return An array of {@link LocalQuickFix} instances, or {@code null} if no fixes are available
     */
    private LocalQuickFix @Nullable [] quickFixes(@Nullable final String sourceCheck) {
        List<LocalQuickFix> fixes = new ArrayList<>();

        if (sourceCheck != null) {
            fixes.add(new SuppressForCheckstyleFix(sourceCheck));
        }

        return addLocalQuickFixes(fixes);
    }

    /**
     * Adds local quick fixes from registered extensions to the provided list.
     * <p>
     * This method queries the {@code CheckstyleQuickFixProvider} extension point for all registered providers
     * and collects their quick fixes. If no fixes are found, returns {@code null}; otherwise, returns an array
     * of all collected fixes.
     *
     * @param fixes The list to which collected quick fixes will be added
     * @return An array of {@link LocalQuickFix} instances, or {@code null} if no fixes are available
     */
    @NotNull
    private LocalQuickFix @Nullable [] addLocalQuickFixes(@NotNull final List<LocalQuickFix> fixes) {
        final Project project = target.getProject();
        if (!project.isDisposed()) {
            try {
                for (CheckstyleQuickFixProvider provider : QUICK_FIX_PROVIDER_EP.getExtensions(project)) {
                    final LocalQuickFix[] providedFixes = provider.getQuickFixes(this);
                    if (providedFixes != null && providedFixes.length > 0) {
                        fixes.addAll(Arrays.asList(providedFixes));
                    }
                }
            } catch (Exception ignored) {
                // The extension point remains backward compatible when not available.
            }
        }

        if (fixes.isEmpty()) {
            return null;
        }
        return fixes.toArray(new LocalQuickFix[0]);
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
