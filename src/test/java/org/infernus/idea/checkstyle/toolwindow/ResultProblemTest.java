package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import org.infernus.idea.checkstyle.checker.ConfigurationLocationResult;
import org.infernus.idea.checkstyle.checker.ConfigurationLocationStatus;
import org.infernus.idea.checkstyle.checker.Problem;
import org.infernus.idea.checkstyle.csapi.SeverityLevel;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResultProblemTest {

    @Mock private Module module;
    @Mock private PsiElement psiElement;
    @Mock private ConfigurationLocation configurationLocation;

    private Problem problem(String sourceName, String message) {
        return new Problem(psiElement, message, SeverityLevel.Warning, 1, 0, sourceName, false, false);
    }

    @Test
    void sourceCheckReturnsShortClassNameWithCheckSuffixStripped() {
        ConfigurationLocationResult locationResult = ConfigurationLocationResult.of(configurationLocation, ConfigurationLocationStatus.PRESENT);
        ResultProblem rp = new ResultProblem(locationResult, module, problem("com.example.NeedBracesCheck", "msg"));
        assertThat(rp.sourceCheck(), is("NeedBraces"));
    }

    @Test
    void locationDescriptionReturnsLocationDescriptionWhenPresent() {
        when(configurationLocation.getDescription()).thenReturn("My Config");
        ConfigurationLocationResult locationResult = ConfigurationLocationResult.of(configurationLocation, ConfigurationLocationStatus.PRESENT);
        ResultProblem rp = new ResultProblem(locationResult, module, problem("com.example.Check", "msg"));
        assertThat(rp.locationDescription(), is("My Config"));
    }

    @Test
    void constructorMapsAllProblemFields() {
        ConfigurationLocationResult locationResult = ConfigurationLocationResult.of(configurationLocation, ConfigurationLocationStatus.PRESENT);
        Problem p = problem("com.example.FooCheck", "test message");
        ResultProblem rp = new ResultProblem(locationResult, module, p);

        assertThat(rp.severityLevel(), is(SeverityLevel.Warning));
        assertThat(rp.line(), is(1));
        assertThat(rp.column(), is(0));
        assertThat(rp.sourceName(), is("com.example.FooCheck"));
        assertThat(rp.message(), is("test message"));
        assertThat(rp.target(), is(psiElement));
        assertThat(rp.module(), is(module));
    }
}
