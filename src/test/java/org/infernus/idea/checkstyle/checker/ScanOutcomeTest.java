package org.infernus.idea.checkstyle.checker;

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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScanOutcomeTest {

    @Mock private ConfigurationLocation configurationLocation;
    @Mock private ConfigurationLocation otherConfigurationLocation;

    private ScanResult resultWith(final ConfigurationLocationResult locationResult) {
        return new ScanResult(locationResult, null, Collections.<PsiFile, List<Problem>>emptyMap(), Set.of());
    }

    private ScanResult presentResult() {
        return resultWith(ConfigurationLocationResult.of(configurationLocation, ConfigurationLocationStatus.PRESENT));
    }

    private ScanResult blockedResult(final ConfigurationLocation location) {
        return resultWith(ConfigurationLocationResult.of(location, ConfigurationLocationStatus.BLOCKED));
    }

    @Test
    void aResultForAPresentRulesFileIsValid() {
        ScanResult present = presentResult();

        assertThat(ScanOutcome.of(List.of(present)).validResults(), contains(present));
    }

    @Test
    void aResultForAnAbsentRulesFileIsNotValid() {
        ScanOutcome outcome = ScanOutcome.of(List.of(resultWith(ConfigurationLocationResult.NOT_PRESENT)));

        assertThat(outcome.validResults(), is(Collections.emptyList()));
    }

    @Test
    void aResultForABlockedRulesFileIsNotValid() {
        ScanOutcome outcome = ScanOutcome.of(List.of(blockedResult(configurationLocation)));

        assertThat(outcome.validResults(), is(Collections.emptyList()));
    }

    @Test
    void aResultWithNoRulesFileAtAllIsNotValid() {
        assertThat(ScanOutcome.of(List.of(ScanResult.EMPTY)).validResults(), is(Collections.emptyList()));
    }

    @Test
    void noRulesFilesAreBlockedWhenEveryRulesFileIsPresent() {
        assertThat(ScanOutcome.of(List.of(presentResult())).hasBlockedRulesFiles(), is(false));
    }

    @Test
    void aBlockedRulesFileIsReportedAsBlocked() {
        assertThat(ScanOutcome.of(List.of(blockedResult(configurationLocation))).hasBlockedRulesFiles(), is(true));
    }

    @Test
    void thereIsNoWarningWhenEveryRulesFileIsPresent() {
        assertThat(ScanOutcome.of(List.of(presentResult())).warningMessage(), is(emptyString()));
    }

    @Test
    void anAbsentRulesFileIsWarnedAbout() {
        ScanOutcome outcome = ScanOutcome.of(List.of(resultWith(ConfigurationLocationResult.NOT_PRESENT)));

        assertThat(outcome.warningMessage(), containsString("No rules file"));
    }

    @Test
    void aBlockedRulesFileIsWarnedAboutByDescription() {
        when(configurationLocation.getDescription()).thenReturn("the-rules");
        when(configurationLocation.blockedForSeconds()).thenReturn(30L);

        ScanOutcome outcome = ScanOutcome.of(List.of(blockedResult(configurationLocation)));

        assertThat(outcome.warningMessage(), containsString("the-rules"));
    }

    @Test
    void theLongestRemainingBlockIsWarnedAbout() {
        when(configurationLocation.getDescription()).thenReturn("the-rules");
        when(configurationLocation.blockedForSeconds()).thenReturn(30L);
        when(otherConfigurationLocation.getDescription()).thenReturn("other-rules");
        when(otherConfigurationLocation.blockedForSeconds()).thenReturn(90L);

        ScanOutcome outcome = ScanOutcome.of(
                List.of(blockedResult(configurationLocation), blockedResult(otherConfigurationLocation)));

        assertThat(outcome.warningMessage(), containsString("90s"));
    }

    @Test
    void warningsAboutAbsentAndBlockedRulesFilesAreCombined() {
        when(configurationLocation.getDescription()).thenReturn("the-rules");
        when(configurationLocation.blockedForSeconds()).thenReturn(30L);

        ScanOutcome outcome = ScanOutcome.of(List.of(
                resultWith(ConfigurationLocationResult.NOT_PRESENT),
                blockedResult(configurationLocation)));

        assertThat(outcome.warningMessage(), containsString("No rules file"));
        assertThat(outcome.warningMessage(), containsString("the-rules"));
    }
}
