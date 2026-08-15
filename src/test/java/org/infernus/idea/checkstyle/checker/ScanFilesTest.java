package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ScanResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScanFilesTest {

    @Mock private Module module;
    @Mock private ConfigurationLocation blockedLocation;
    @Mock private ConfigurationLocation availableLocation;
    @Mock private PsiFile fileA;
    @Mock private PsiFile fileB;
    @Mock private PsiFile unreadableFile;

    // --- reporting the rules files that were not scanned against ---

    @Test
    void aBlockedRulesFileIsReportedSoThatItsPreviousFindingsAreNotSilentlyDiscarded() {
        List<ConfigurationLocationResult> locationResults = List.of(
                ConfigurationLocationResult.of(blockedLocation, ConfigurationLocationStatus.BLOCKED));

        List<ScanResult> blocked = ScanFiles.blockedResultsIn(locationResults, module);

        assertThat(blocked, hasSize(1));
        assertThat(blocked.getFirst().configurationLocationResult().location(), is(blockedLocation));
    }

    @Test
    void aBlockedRulesFileIsAuthoritativeForNothing() {
        List<ScanResult> blocked = ScanFiles.blockedResultsIn(
                List.of(ConfigurationLocationResult.of(blockedLocation, ConfigurationLocationStatus.BLOCKED)), module);

        assertThat(blocked.getFirst().scannedFiles(), is(empty()));
        assertThat(blocked.getFirst().problems().isEmpty(), is(true));
    }

    @Test
    void anAvailableRulesFileIsNotReportedAsBlocked() {
        List<ConfigurationLocationResult> locationResults = List.of(
                ConfigurationLocationResult.of(availableLocation, ConfigurationLocationStatus.PRESENT));

        assertThat(ScanFiles.blockedResultsIn(locationResults, module), is(empty()));
    }

    // --- the files a scan can speak for ---

    @Test
    void aFileThatWasScannedIsIncluded() {
        Set<PsiFile> authoritativeFor = ScanFiles.filesTheScanIsAuthoritativeFor(
                Set.of(fileA), Set.of(fileA), List.of(scannableFileFor(fileA)));

        assertThat(authoritativeFor, contains(fileA));
    }

    @Test
    void aFileSkippedAsOutOfScopeIsIncludedSoThatItsStaleResultsAreRemoved() {
        Set<PsiFile> authoritativeFor = ScanFiles.filesTheScanIsAuthoritativeFor(
                Set.of(fileA, fileB), Set.of(fileA), List.of(scannableFileFor(fileA)));

        assertThat(authoritativeFor, containsInAnyOrder(fileA, fileB));
    }

    @Test
    void aFileInScopeThatCouldNotBeReadIsExcludedSoThatItKeepsItsResults() {
        // in scope, so passed to the scan, but no ScannableFile came back for it
        Set<PsiFile> authoritativeFor = ScanFiles.filesTheScanIsAuthoritativeFor(
                Set.of(fileA, unreadableFile), Set.of(fileA, unreadableFile), List.of(scannableFileFor(fileA)));

        assertThat(authoritativeFor, contains(fileA));
    }

    @Test
    void aScanOfNothingIsAuthoritativeForNothing() {
        assertThat(ScanFiles.filesTheScanIsAuthoritativeFor(Set.of(), Set.of(), List.of()), is(empty()));
    }

    @Test
    void everyFileIsIncludedWhenNoneOfThemWereInScope() {
        Set<PsiFile> authoritativeFor = ScanFiles.filesTheScanIsAuthoritativeFor(
                Set.of(fileA, fileB), Collections.emptySet(), List.of());

        assertThat(authoritativeFor, containsInAnyOrder(fileA, fileB));
    }

    private ScannableFile scannableFileFor(final PsiFile psiFile) {
        final ScannableFile scannableFile = mock(ScannableFile.class);
        when(scannableFile.getPsiFile()).thenReturn(psiFile);
        return scannableFile;
    }
}
