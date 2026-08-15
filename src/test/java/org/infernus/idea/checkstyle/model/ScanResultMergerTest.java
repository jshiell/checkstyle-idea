package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScanResultMergerTest {

    @Mock private Module module;
    @Mock private ConfigurationLocation configurationLocation;
    @Mock private PsiElement psiElement;

    @Mock private PsiFile fileA;
    @Mock private PsiFile fileB;
    @Mock private PsiFile reloadedFileA;
    @Mock private VirtualFile virtualFileA;
    @Mock private VirtualFile virtualFileB;

    private Problem problem(final String message) {
        return new Problem(psiElement, message, SeverityLevel.Warning, 1, 0, "com.example.FooCheck", false, false);
    }

    private ScanResult resultOf(final Map<PsiFile, List<Problem>> problems,
                                final Set<PsiFile> scannedFiles) {
        return new ScanResult(
                ConfigurationLocationResult.of(configurationLocation, ConfigurationLocationStatus.PRESENT),
                module,
                problems,
                scannedFiles);
    }

    private Map<PsiFile, List<Problem>> problemsIn(final List<ScanResult> results) {
        final Map<PsiFile, List<Problem>> flattened = new HashMap<>();
        results.forEach(result -> result.problems().forEach(
                (file, problems) -> flattened.computeIfAbsent(file, key -> new ArrayList<>()).addAll(problems)));
        return flattened;
    }

    @Test
    void aRescannedFileShowsOnlyItsLatestProblems() {
        Problem stale = problem("stale");
        Problem fresh = problem("fresh");
        List<ScanResult> previous = List.of(resultOf(Map.of(fileA, List.of(stale)), Set.of()));
        List<ScanResult> latest = List.of(resultOf(Map.of(fileA, List.of(fresh)), Set.of(fileA)));

        when(fileA.getVirtualFile()).thenReturn(virtualFileA);

        assertThat(problemsIn(ScanResultMerger.merge(previous, latest)).get(fileA), contains(fresh));
    }

    @Test
    void aRescannedFileWithNoRemainingProblemsDisappears() {
        List<ScanResult> previous = List.of(resultOf(Map.of(fileA, List.of(problem("stale"))), Set.of()));
        List<ScanResult> latest = List.of(resultOf(Collections.emptyMap(), Set.of(fileA)));

        when(fileA.getVirtualFile()).thenReturn(virtualFileA);

        assertThat(problemsIn(ScanResultMerger.merge(previous, latest)), not(hasKey(fileA)));
    }

    @Test
    void filesThatWereNotRescannedKeepTheirProblems() {
        Problem untouched = problem("untouched");
        List<ScanResult> previous = List.of(resultOf(
                Map.of(fileA, List.of(problem("stale")), fileB, List.of(untouched)), Set.of()));
        List<ScanResult> latest = List.of(resultOf(Collections.emptyMap(), Set.of(fileA)));

        when(fileA.getVirtualFile()).thenReturn(virtualFileA);
        when(fileB.getVirtualFile()).thenReturn(virtualFileB);

        assertThat(problemsIn(ScanResultMerger.merge(previous, latest)).get(fileB), contains(untouched));
    }

    @Test
    void aReloadedPsiFileForTheSameVirtualFileReplacesRatherThanDuplicates() {
        Problem fresh = problem("fresh");
        List<ScanResult> previous = List.of(resultOf(Map.of(fileA, List.of(problem("stale"))), Set.of()));
        List<ScanResult> latest = List.of(resultOf(Map.of(reloadedFileA, List.of(fresh)), Set.of(reloadedFileA)));

        when(fileA.getVirtualFile()).thenReturn(virtualFileA);
        when(reloadedFileA.getVirtualFile()).thenReturn(virtualFileA);

        Map<PsiFile, List<Problem>> merged = problemsIn(ScanResultMerger.merge(previous, latest));

        assertThat(merged, not(hasKey(fileA)));
        assertThat(merged.get(reloadedFileA), contains(fresh));
    }

    @Test
    void aFileWithNoVirtualFileIsNeverConsideredRescanned() {
        Problem retained = problem("retained");
        List<ScanResult> previous = List.of(resultOf(Map.of(fileA, List.of(retained)), Set.of()));
        List<ScanResult> latest = List.of(resultOf(Collections.emptyMap(), Set.of(fileB)));

        assertThat(problemsIn(ScanResultMerger.merge(previous, latest)).get(fileA), contains(retained));
    }

    @Test
    void aPreviousEntryWithNoProblemsIsRemovedRatherThanRetainedEmpty() {
        List<ScanResult> previous = List.of(resultOf(
                Map.of(fileA, Collections.emptyList(), fileB, List.of(problem("real"))), Set.of()));
        List<ScanResult> latest = List.of(resultOf(Collections.emptyMap(), Set.of()));

        assertThat(problemsIn(ScanResultMerger.merge(previous, latest)), not(hasKey(fileA)));
    }

    @Test
    void aPreviousResultLeftWithNoProblemsIsDropped() {
        List<ScanResult> previous = List.of(resultOf(Map.of(fileA, List.of(problem("stale"))), Set.of()));
        List<ScanResult> latest = List.of(resultOf(Collections.emptyMap(), Set.of(fileA)));

        when(fileA.getVirtualFile()).thenReturn(virtualFileA);

        assertThat(ScanResultMerger.merge(previous, latest), is(latest));
    }

    @Test
    void latestResultsAreAppendedToTheSurvivingPreviousResults() {
        List<ScanResult> previous = List.of(resultOf(Map.of(fileB, List.of(problem("untouched"))), Set.of()));
        List<ScanResult> latest = List.of(resultOf(Map.of(fileA, List.of(problem("fresh"))), Set.of(fileA)));

        when(fileA.getVirtualFile()).thenReturn(virtualFileA);
        when(fileB.getVirtualFile()).thenReturn(virtualFileB);

        assertThat(ScanResultMerger.merge(previous, latest), hasSize(2));
    }

    @Test
    void anEmptyPreviousResultDoesNotFail() {
        List<ScanResult> latest = List.of(resultOf(Map.of(fileA, List.of(problem("fresh"))), Set.of(fileA)));

        when(fileA.getVirtualFile()).thenReturn(virtualFileA);

        assertThat(ScanResultMerger.merge(List.of(ScanResult.EMPTY), latest), is(latest));
    }

    @Test
    void mergingIntoNoPreviousResultsYieldsTheLatestResults() {
        List<ScanResult> latest = List.of(resultOf(Map.of(fileA, List.of(problem("fresh"))), Set.of(fileA)));

        when(fileA.getVirtualFile()).thenReturn(virtualFileA);

        assertThat(ScanResultMerger.merge(Collections.emptyList(), latest), is(latest));
    }
}
