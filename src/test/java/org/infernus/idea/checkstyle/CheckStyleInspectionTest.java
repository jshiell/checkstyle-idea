package org.infernus.idea.checkstyle;

import com.intellij.psi.PsiElement;
import org.infernus.idea.checkstyle.checker.Problem;
import org.infernus.idea.checkstyle.csapi.SeverityLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for the pure-logic methods of {@link CheckStyleInspection}.
 *
 * <p>{@code checkFile()} and its helpers that call into the IntelliJ platform
 * ({@code InjectedLanguageManager}, {@code ModuleUtil}, {@code ScannableFile.createAndValidate()},
 * {@code asyncResultOf()}) require a live IDEA application and are covered at the
 * integration level only.
 */
@ExtendWith(MockitoExtension.class)
class CheckStyleInspectionTest {

    @Mock
    private PsiElement psiElement;

    private final CheckStyleInspection underTest = new CheckStyleInspection();

    @Test
    void dropIgnoredProblemsKeepsProblemsAboveIgnoreLevel() {
        List<Problem> input = List.of(
                problem(SeverityLevel.Error),
                problem(SeverityLevel.Warning),
                problem(SeverityLevel.Info)
        );

        List<Problem> result = underTest.dropIgnoredProblems(input);

        assertThat(result, hasSize(3));
    }

    @Test
    void dropIgnoredProblemsRemovesIgnoreLevelProblems() {
        List<Problem> input = List.of(
                problem(SeverityLevel.Error),
                problem(SeverityLevel.Ignore)
        );

        List<Problem> result = underTest.dropIgnoredProblems(input);

        assertThat(result, hasSize(1));
        assertThat(result.get(0).severityLevel(), is(SeverityLevel.Error));
    }

    @Test
    void dropIgnoredProblemsReturnsEmptyListWhenAllProblemsAreIgnored() {
        List<Problem> input = List.of(
                problem(SeverityLevel.Ignore),
                problem(SeverityLevel.Ignore)
        );

        List<Problem> result = underTest.dropIgnoredProblems(input);

        assertThat(result, is(empty()));
    }

    @Test
    void dropIgnoredProblemsReturnsEmptyListForEmptyInput() {
        List<Problem> result = underTest.dropIgnoredProblems(List.of());

        assertThat(result, is(empty()));
    }

    @Test
    void dropIgnoredProblemsDoesNotMutateTheInputList() {
        List<Problem> input = List.of(
                problem(SeverityLevel.Error),
                problem(SeverityLevel.Ignore)
        );

        underTest.dropIgnoredProblems(input);

        assertThat(input, hasSize(2));
    }

    private Problem problem(final SeverityLevel severityLevel) {
        return new Problem(psiElement, "message", severityLevel, 1, 1, "SourceCheck", false, false);
    }
}
