package org.infernus.idea.checkstyle.checker;

import com.intellij.psi.PsiElement;
import org.infernus.idea.checkstyle.csapi.SeverityLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

@ExtendWith(MockitoExtension.class)
class ProblemTest {

    @Mock private PsiElement psiElement;

    private Problem problem(int line, int column, SeverityLevel severity, String message) {
        return new Problem(psiElement, message, severity, line, column, "com.example.FooCheck", false, false);
    }

    @Test
    void problemsOnDifferentLinesOrderByLine() {
        Problem first = problem(1, 0, SeverityLevel.Warning, "msg");
        Problem second = problem(2, 0, SeverityLevel.Warning, "msg");
        assertThat(first.compareTo(second), is(lessThan(0)));
        assertThat(second.compareTo(first), is(greaterThan(0)));
    }

    @Test
    void problemsOnSameLineOrderByColumn() {
        Problem first = problem(1, 3, SeverityLevel.Warning, "msg");
        Problem second = problem(1, 7, SeverityLevel.Warning, "msg");
        assertThat(first.compareTo(second), is(lessThan(0)));
    }

    @Test
    void problemsAtSamePositionOrderBySeverityDescending() {
        Problem error = problem(1, 0, SeverityLevel.Error, "msg");
        Problem info = problem(1, 0, SeverityLevel.Info, "msg");
        // higher severity (Error > Info) should sort first (negative compareTo)
        assertThat(error.compareTo(info), is(lessThan(0)));
    }

    @Test
    void problemsAtSamePositionAndSeverityOrderByMessage() {
        Problem a = problem(1, 0, SeverityLevel.Warning, "aaa");
        Problem b = problem(1, 0, SeverityLevel.Warning, "bbb");
        assertThat(a.compareTo(b), is(lessThan(0)));
    }

    @Test
    void identicalProblemsCompareAsEqual() {
        Problem p = problem(5, 3, SeverityLevel.Error, "msg");
        assertThat(p.compareTo(p), is(0));
    }
}
