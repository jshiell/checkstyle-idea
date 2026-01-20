package org.infernus.idea.checkstyle.checker;

import com.intellij.codeInspection.LocalQuickFix;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for providing quick fixes for Checkstyle problems.
 * <p>This interface allows plugins or extensions to register custom quick fixes that can be applied to Checkstyle violations.
 * Implementations should return an array of {@code LocalQuickFix} objects that represent the available fixes for a given problem.
 * The {@code getQuickFixes} method is called by the Checkstyle inspection engine when a problem is detected.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 26.0.0
 */
public interface CheckstyleQuickFixProvider {

    /**
     * Provides additional quick fixes for a given Checkstyle problem.
     * <p>Implementations should return an array of {@code LocalQuickFix} objects that offer
     * applicable fixes for the specified {@code Problem}. The return value may be {@code null}
     * if no quick fixes are available for the problem.
     *
     * @param problem the Checkstyle problem for which quick fixes are requested; must not be null
     * @return an array of quick fixes, which may be null if none are applicable
     */
    @Nullable
    LocalQuickFix[] getQuickFixes(@NotNull Problem problem);
}
