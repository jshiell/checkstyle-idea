package org.infernus.idea.checkstyle.toolwindow;

import org.infernus.idea.checkstyle.csapi.SeverityLevel;
import org.infernus.idea.checkstyle.exception.CheckStylePluginParseException;
import org.infernus.idea.checkstyle.exception.CheckstyleToolException;
import org.infernus.idea.checkstyle.model.ScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
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

    private ResultTreeBuilder underTest;

    @BeforeEach
    void setUp() {
        underTest = new ResultTreeBuilder(treeModel, progressManager, navigator);
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
}
