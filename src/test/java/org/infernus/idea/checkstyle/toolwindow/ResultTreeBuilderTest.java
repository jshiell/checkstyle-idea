package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.checker.ConfigurationLocationResult;
import org.infernus.idea.checkstyle.checker.ConfigurationLocationStatus;
import org.infernus.idea.checkstyle.checker.Problem;
import org.infernus.idea.checkstyle.csapi.SeverityLevel;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.exception.CheckStylePluginParseException;
import org.infernus.idea.checkstyle.exception.CheckstyleToolException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class ResultTreeBuilderTest {

    @Mock
    private ResultTreeModel treeModel;
    @Mock
    private ScanProgressManager progressManager;
    @Mock
    private ResultTreeNavigator navigator;
    @Mock
    private ConfigurationLocation configurationLocation;
    @Mock
    private PsiElement psiElement;
    @Mock
    private PsiFile fileA;
    @Mock
    private PsiFile fileB;
    @Mock
    private VirtualFile virtualFileA;
    @Mock
    private VirtualFile virtualFileB;

    private ResultTreeBuilder underTest;

    @BeforeEach
    void setUp() {
        underTest = new ResultTreeBuilder(treeModel, progressManager, navigator);
    }

    private ConfigurationLocationResult presentLocation() {
        return ConfigurationLocationResult.of(configurationLocation, ConfigurationLocationStatus.PRESENT);
    }

    private Problem problem() {
        return new Problem(psiElement, "msg", SeverityLevel.Warning, 1, 0, "com.example.FooCheck", false, false);
    }

    private ScanResult scanResultWith(final ConfigurationLocationResult locationResult) {
        return scanResultWith(locationResult, Collections.emptyMap(), Set.of());
    }

    private ScanResult scanResultWith(final ConfigurationLocationResult locationResult,
                                      final Map<PsiFile, List<Problem>> problems,
                                      final Set<PsiFile> scannedFiles) {
        return new ScanResult(locationResult, null, problems, scannedFiles);
    }

    // --- severity filter state ---

    @Test
    void byDefaultAllSeveritiesAreDisplayed() {
        Set<SeverityLevel> severities = underTest.getDisplayedSeverities();
        assertThat(severities, containsInAnyOrder(SeverityLevel.Error, SeverityLevel.Warning, SeverityLevel.Info));
    }

    @Test
    void disablingErrorsExcludesErrorSeverity() {
        underTest.setDisplayingErrors(false);
        assertThat(underTest.getDisplayedSeverities(), not(hasItem(SeverityLevel.Error)));
    }

    @Test
    void disablingWarningsExcludesWarningSeverity() {
        underTest.setDisplayingWarnings(false);
        assertThat(underTest.getDisplayedSeverities(), not(hasItem(SeverityLevel.Warning)));
    }

    @Test
    void disablingInfoExcludesInfoSeverity() {
        underTest.setDisplayingInfo(false);
        assertThat(underTest.getDisplayedSeverities(), not(hasItem(SeverityLevel.Info)));
    }

    @Test
    void disablingAllSeveritiesProducesEmptySet() {
        underTest.setDisplayingErrors(false);
        underTest.setDisplayingWarnings(false);
        underTest.setDisplayingInfo(false);
        assertThat(underTest.getDisplayedSeverities(), is(empty()));
    }

    @Test
    void enablingOnlyErrorsProducesSingletonSet() {
        underTest.setDisplayingWarnings(false);
        underTest.setDisplayingInfo(false);
        assertThat(underTest.getDisplayedSeverities(), containsInAnyOrder(SeverityLevel.Error));
    }

    // --- getter/setter round-trips ---

    @Test
    void isDisplayingErrorsDefaultsTrue() {
        assertThat(underTest.isDisplayingErrors(), is(true));
    }

    @Test
    void isDisplayingWarningsDefaultsTrue() {
        assertThat(underTest.isDisplayingWarnings(), is(true));
    }

    @Test
    void isDisplayingInfoDefaultsTrue() {
        assertThat(underTest.isDisplayingInfo(), is(true));
    }

    @Test
    void setDisplayingErrorsIsReflectedByGetter() {
        underTest.setDisplayingErrors(false);
        assertThat(underTest.isDisplayingErrors(), is(false));
    }

    @Test
    void setDisplayingWarningsIsReflectedByGetter() {
        underTest.setDisplayingWarnings(false);
        assertThat(underTest.isDisplayingWarnings(), is(false));
    }

    @Test
    void setDisplayingInfoIsReflectedByGetter() {
        underTest.setDisplayingInfo(false);
        assertThat(underTest.isDisplayingInfo(), is(false));
    }

    // --- delegation to treeModel / progressManager / navigator ---

    @Test
    void displayInProgressSetsProgressBarMaxAndClearsTree() {
        underTest.displayInProgress(42);

        verify(progressManager).setProgressBarMax(42);
        verify(treeModel).clear();
        verify(treeModel).setRootMessage("plugin.results.in-progress");
    }

    @Test
    void displayWarningResultClearsProgressAndSetsRootMessage() {
        underTest.displayWarningResult("some.key", "arg1");

        verify(progressManager).clearProgress();
        verify(treeModel).clear();
        verify(treeModel).setRootMessage("some.key", "arg1");
    }

    @Test
    void displayResultsDelegatesToModelAndNavigator() {
        List<ScanResult> results = Collections.emptyList();

        underTest.displayResults(results, null);

        verify(treeModel).setModel(eq(results), any());
        verify(progressManager).clearProgress();
        verify(navigator).expandTree(treeModel, 3);
    }

    @Test
    void displayResultsWithWarningMessageSetsProgressText() {
        List<ScanResult> results = Collections.emptyList();

        underTest.displayResults(results, "a warning");

        verify(progressManager).setProgressText("a warning");
    }

    @Test
    void displayResultsDiscardsResultsWithNoRulesFile() {
        underTest.displayResults(List.of(ScanResult.EMPTY), null);

        verify(treeModel).setModel(eq(List.of()), any());
    }

    @Test
    void displayResultsDiscardsResultsForAbsentRulesFiles() {
        ScanResult absent = scanResultWith(ConfigurationLocationResult.NOT_PRESENT);

        underTest.displayResults(List.of(absent), null);

        verify(treeModel).setModel(eq(List.of()), any());
    }

    @Test
    void displayResultsDiscardsResultsForBlockedRulesFiles() {
        ScanResult blocked = scanResultWith(
                ConfigurationLocationResult.of(configurationLocation, ConfigurationLocationStatus.BLOCKED));

        underTest.displayResults(List.of(blocked), null);

        verify(treeModel).setModel(eq(List.of()), any());
    }

    @Test
    void displayResultsRetainsResultsForPresentRulesFiles() {
        ScanResult present = scanResultWith(
                ConfigurationLocationResult.of(configurationLocation, ConfigurationLocationStatus.PRESENT));

        underTest.displayResults(List.of(present), null);

        verify(treeModel).setModel(eq(List.of(present)), any());
    }

    // --- retained results ---

    @Test
    void noResultsAreRetainedBeforeAnyScan() {
        assertThat(underTest.lastScanResults(), is(empty()));
    }

    @Test
    void displayResultsRetainsTheDisplayedResults() {
        ScanResult present = scanResultWith(
                ConfigurationLocationResult.of(configurationLocation, ConfigurationLocationStatus.PRESENT));

        underTest.displayResults(List.of(present), null);

        assertThat(underTest.lastScanResults(), contains(present));
    }

    @Test
    void displayResultsRetainsOnlyTheResultsWorthDisplaying() {
        underTest.displayResults(List.of(ScanResult.EMPTY), null);

        assertThat(underTest.lastScanResults(), is(empty()));
    }

    @Test
    void displayInProgressDiscardsTheRetainedResults() {
        underTest.displayResults(List.of(scanResultWith(
                ConfigurationLocationResult.of(configurationLocation, ConfigurationLocationStatus.PRESENT))), null);

        underTest.displayInProgress(1);

        assertThat(underTest.lastScanResults(), is(empty()));
    }

    @Test
    void displayWarningResultDiscardsTheRetainedResults() {
        underTest.displayResults(List.of(scanResultWith(
                ConfigurationLocationResult.of(configurationLocation, ConfigurationLocationStatus.PRESENT))), null);

        underTest.displayWarningResult("some.key");

        assertThat(underTest.lastScanResults(), is(empty()));
    }

    @Test
    void displayErrorResultDiscardsTheRetainedResults() {
        underTest.displayResults(List.of(scanResultWith(
                ConfigurationLocationResult.of(configurationLocation, ConfigurationLocationStatus.PRESENT))), null);

        underTest.displayErrorResult(new RuntimeException("some error"));

        assertThat(underTest.lastScanResults(), is(empty()));
    }

    // --- results available to refresh ---

    @Test
    void thereAreNoResultsBeforeAnyScan() {
        assertThat(underTest.hasResults(), is(false));
    }

    @Test
    void thereAreResultsOnceAScanHasFoundProblems() {
        underTest.displayResults(
                List.of(scanResultWith(presentLocation(), Map.of(fileA, List.of(problem())), Set.of(fileA))), null);

        assertThat(underTest.hasResults(), is(true));
    }

    @Test
    void thereAreNoResultsWhenEveryProblemFoundIsIgnored() {
        Problem ignored = new Problem(psiElement, "msg", SeverityLevel.Ignore, 1, 0, "com.example.FooCheck", false, false);
        underTest.displayResults(
                List.of(scanResultWith(presentLocation(), Map.of(fileA, List.of(ignored)), Set.of(fileA))), null);

        assertThat(underTest.hasResults(), is(false));
    }

    @Test
    void thereAreNoResultsWhenAScanFoundNothing() {
        underTest.displayResults(
                List.of(scanResultWith(presentLocation(), Collections.emptyMap(), Set.of(fileA))), null);

        assertThat(underTest.hasResults(), is(false));
    }

    @Test
    void discardingResultsForAFileRemovesItsProblems() {
        underTest.displayResults(List.of(scanResultWith(presentLocation(),
                Map.of(fileA, List.of(problem()), fileB, List.of(problem())), Set.of(fileA, fileB))), null);

        underTest.discardResultsFor(Set.of(fileA));

        assertThat(underTest.lastScanResults().getFirst().problems(), not(hasKey(fileA)));
    }

    @Test
    void discardingResultsForAFileLeavesTheOtherFilesAlone() {
        underTest.displayResults(List.of(scanResultWith(presentLocation(),
                Map.of(fileA, List.of(problem()), fileB, List.of(problem())), Set.of(fileA, fileB))), null);

        underTest.discardResultsFor(Set.of(fileA));

        assertThat(underTest.lastScanResults().getFirst().problems(), hasKey(fileB));
    }

    @Test
    void discardingTheLastResultsForAScanDropsThatScanEntirely() {
        underTest.displayResults(
                List.of(scanResultWith(presentLocation(), Map.of(fileA, List.of(problem())), Set.of(fileA))), null);

        underTest.discardResultsFor(Set.of(fileA));

        assertThat(underTest.lastScanResults(), is(empty()));
    }

    @Test
    void discardingResultsRedisplaysTheTree() {
        underTest.displayResults(
                List.of(scanResultWith(presentLocation(), Map.of(fileA, List.of(problem())), Set.of(fileA))), null);

        underTest.discardResultsFor(Set.of(fileA));

        verify(treeModel).setModel(eq(List.of()), any());
    }

    @Test
    void discardingResultsForNoFilesLeavesTheResultsAlone() {
        ScanResult present = scanResultWith(presentLocation(), Map.of(fileA, List.of(problem())), Set.of(fileA));
        underTest.displayResults(List.of(present), null);

        underTest.discardResultsFor(Set.of());

        assertThat(underTest.lastScanResults(), contains(present));
    }

    // --- merging a re-scan into the displayed results ---

    @Test
    void mergeResultsCombinesTheLatestResultsWithThoseRetained() {
        ScanResult previous = scanResultWith(presentLocation(), Map.of(fileB, List.of(problem())), Set.of());
        ScanResult latest = scanResultWith(presentLocation(), Map.of(fileA, List.of(problem())), Set.of(fileA));
        underTest.displayResults(List.of(previous), null);

        when(fileA.getVirtualFile()).thenReturn(virtualFileA);
        when(fileB.getVirtualFile()).thenReturn(virtualFileB);

        underTest.mergeResults(List.of(latest), null);

        verify(treeModel).setModel(eq(List.of(previous, latest)), any());
    }

    @Test
    void mergeResultsRetainsTheMergedResults() {
        ScanResult latest = scanResultWith(presentLocation(), Map.of(fileA, List.of(problem())), Set.of(fileA));

        when(fileA.getVirtualFile()).thenReturn(virtualFileA);

        underTest.mergeResults(List.of(latest), null);

        assertThat(underTest.lastScanResults(), contains(latest));
    }

    @Test
    void mergeResultsDiscardsTheStaleResultsForARescannedFile() {
        ScanResult previous = scanResultWith(presentLocation(), Map.of(fileA, List.of(problem())), Set.of());
        ScanResult latest = scanResultWith(presentLocation(), Collections.emptyMap(), Set.of(fileA));
        underTest.displayResults(List.of(previous), null);

        when(fileA.getVirtualFile()).thenReturn(virtualFileA);

        underTest.mergeResults(List.of(latest), null);

        verify(treeModel).setModel(eq(List.of(latest)), any());
    }

    @Test
    void mergeResultsClearsProgressAndExpandsTheTree() {
        underTest.mergeResults(Collections.emptyList(), null);

        verify(progressManager).clearProgress();
        verify(navigator).expandTree(treeModel, 3);
    }

    @Test
    void mergeResultsWithWarningMessageSetsProgressText() {
        underTest.mergeResults(Collections.emptyList(), "a warning");

        verify(progressManager).setProgressText("a warning");
    }

    @Test
    void mergeResultsNeverClearsTheTree() {
        underTest.mergeResults(Collections.emptyList(), null);

        verify(treeModel, never()).clear();
    }

    @Test
    void displayRefreshInProgressSetsTheProgressBarMax() {
        underTest.displayRefreshInProgress(42);

        verify(progressManager).setProgressBarMax(42);
    }

    @Test
    void displayRefreshInProgressLeavesTheTreeReadable() {
        underTest.displayRefreshInProgress(42);

        verify(treeModel, never()).clear();
        verify(treeModel, never()).setRootMessage(any(), any());
    }

    @Test
    void displayRefreshInProgressKeepsTheRetainedResults() {
        ScanResult present = scanResultWith(presentLocation());
        underTest.displayResults(List.of(present), null);

        underTest.displayRefreshInProgress(1);

        assertThat(underTest.lastScanResults(), contains(present));
    }

    @Test
    void displayResultsWithNullWarningMessageDoesNotSetProgressText() {
        List<ScanResult> results = Collections.emptyList();

        underTest.displayResults(results, null);

        verify(progressManager).clearProgress();
        verify(progressManager, never()).setProgressText(any());
    }

    @Test
    void filterDisplayedResultsDelegatesToModelAndNavigator() {
        underTest.filterDisplayedResults();

        verify(treeModel).filter(any());
        verify(navigator).expandTree(treeModel, 3);
    }

    @Test
    void filterDisplayedResultsPassesCurrentSeveritiesToModel() {
        underTest.setDisplayingWarnings(false);

        underTest.filterDisplayedResults();

        verify(treeModel).filter(Set.of(SeverityLevel.Error, SeverityLevel.Info));
    }

    @Test
    void groupByDelegatesToModel() {
        underTest.groupBy(ResultGrouping.BY_SEVERITY);

        verify(treeModel).groupBy(ResultGrouping.BY_SEVERITY);
    }

    @Test
    void groupedByDelegatesToModel() {
        when(treeModel.groupedBy()).thenReturn(ResultGrouping.BY_SEVERITY);

        assertThat(underTest.groupedBy(), is(ResultGrouping.BY_SEVERITY));
    }

    // --- displayErrorResult ---

    @Test
    void displayErrorResultClearsTreeAndProgress() {
        underTest.displayErrorResult(new RuntimeException("some error"));

        verify(treeModel).clear();
        verify(progressManager).clearProgress();
    }

    @Test
    void displayErrorResultSetsRootTextOnModel() {
        underTest.displayErrorResult(new RuntimeException("some error"));

        ArgumentCaptor<String> rootText = ArgumentCaptor.forClass(String.class);
        verify(treeModel).setRootText(rootText.capture());
        assertThat(rootText.getValue(), containsString("scan failed"));
    }

    @Test
    void displayErrorResultForParseExceptionSetsRootText() {
        underTest.displayErrorResult(new CheckStylePluginParseException("parse failure", null));

        ArgumentCaptor<String> rootText = ArgumentCaptor.forClass(String.class);
        verify(treeModel).setRootText(rootText.capture());
        assertThat(rootText.getValue(), containsString("could not be parsed"));
    }

    @Test
    void displayErrorResultForCheckstyleToolExceptionWithMissingPropertyMatchesPattern() {
        RuntimeException cause = new RuntimeException("Property ${my.property} has not been set");
        CheckstyleToolException toolException = new CheckstyleToolException(cause);

        underTest.displayErrorResult(toolException);

        ArgumentCaptor<String> rootText = ArgumentCaptor.forClass(String.class);
        verify(treeModel).setRootText(rootText.capture());
        assertThat(rootText.getValue(), containsString("my.property"));
    }

    @Test
    void displayErrorResultForCheckstyleToolExceptionWithInstantiationFailureMatchesPattern() {
        RuntimeException cause = new RuntimeException("Unable to instantiate com.example.MyCheck");
        CheckstyleToolException toolException = new CheckstyleToolException(cause);

        underTest.displayErrorResult(toolException);

        ArgumentCaptor<String> rootText = ArgumentCaptor.forClass(String.class);
        verify(treeModel).setRootText(rootText.capture());
        assertThat(rootText.getValue(), containsString("com.example.MyCheck"));
    }

    @Test
    void displayErrorResultForCheckStylePluginExceptionUsesItsOwnMessage() {
        underTest.displayErrorResult(
                new CheckStylePluginException("Failed to download third-party check JAR from https://example.invalid/x.jar"));

        ArgumentCaptor<String> rootText = ArgumentCaptor.forClass(String.class);
        verify(treeModel).setRootText(rootText.capture());
        assertThat(rootText.getValue(),
                containsString("Failed to download third-party check JAR from https://example.invalid/x.jar"));
    }

    @Test
    void displayErrorResultForCheckstyleToolExceptionWithUnmatchedCauseFallsBackToGenericMessage() {
        RuntimeException cause = new RuntimeException("some other internal failure");
        CheckstyleToolException toolException = new CheckstyleToolException(cause);

        underTest.displayErrorResult(toolException);

        ArgumentCaptor<String> rootText = ArgumentCaptor.forClass(String.class);
        verify(treeModel).setRootText(rootText.capture());
        assertThat(rootText.getValue(), containsString("scan failed"));
    }
}
