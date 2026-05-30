package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.checker.ConfigurationLocationResult;
import org.infernus.idea.checkstyle.checker.ConfigurationLocationStatus;
import org.infernus.idea.checkstyle.checker.Problem;
import org.infernus.idea.checkstyle.csapi.SeverityLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@ExtendWith(MockitoExtension.class)
class ScanResultTest {

    @Mock private Module module;
    @Mock private PsiFile psiFile;
    @Mock private PsiElement psiElement;
    @Mock private ConfigurationLocation configurationLocation;

    private Problem problem() {
        return new Problem(psiElement, "msg", SeverityLevel.Warning, 1, 0, "com.example.FooCheck", false, false);
    }

    @Test
    void emptyConstantHasNullConfigurationResult() {
        assertThat(ScanResult.EMPTY.configurationLocationResult(), is(nullValue()));
    }

    @Test
    void emptyConstantHasNullModule() {
        assertThat(ScanResult.EMPTY.module(), is(nullValue()));
    }

    @Test
    void emptyConstantHasNoProblems() {
        assertThat(ScanResult.EMPTY.problems().isEmpty(), is(true));
    }

    @Test
    void constructorStoresAllFields() {
        ConfigurationLocationResult result = ConfigurationLocationResult.of(configurationLocation, ConfigurationLocationStatus.PRESENT);
        Problem p = problem();
        Map<PsiFile, List<Problem>> problems = Map.of(psiFile, List.of(p));

        ScanResult scanResult = new ScanResult(result, module, problems);

        assertThat(scanResult.configurationLocationResult(), is(result));
        assertThat(scanResult.module(), is(module));
        assertThat(scanResult.problems(), is(aMapWithSize(1)));
    }

    @Test
    void problemsMapIsAccessible() {
        ConfigurationLocationResult result = ConfigurationLocationResult.of(configurationLocation, ConfigurationLocationStatus.PRESENT);
        Problem p = problem();
        Map<PsiFile, List<Problem>> problems = Collections.singletonMap(psiFile, List.of(p));

        ScanResult scanResult = new ScanResult(result, module, problems);

        assertThat(scanResult.problems().get(psiFile), is(List.of(p)));
    }
}
