package org.infernus.idea.checkstyle.actions;

import org.infernus.idea.checkstyle.config.ConventionalConfigurationLocationScanner.ScanOutcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DetectConventionalConfigurationLocationTest {

    @Test
    public void messageForNoProjectDirectoryIsEmpty() {
        assertEquals(java.util.Optional.empty(),
                DetectConventionalConfigurationLocation.messageFor(ScanOutcome.NO_PROJECT_DIRECTORY));
    }

    @Test
    public void messageForAddedIsPresent() {
        assertTrue(DetectConventionalConfigurationLocation.messageFor(ScanOutcome.ADDED).isPresent());
    }

    @Test
    public void messageForReplacedIsPresent() {
        assertTrue(DetectConventionalConfigurationLocation.messageFor(ScanOutcome.REPLACED).isPresent());
    }

    @Test
    public void messageForRemovedIsPresent() {
        assertTrue(DetectConventionalConfigurationLocation.messageFor(ScanOutcome.REMOVED).isPresent());
    }

    @Test
    public void messageForUnchangedPresentIsPresent() {
        assertTrue(DetectConventionalConfigurationLocation.messageFor(ScanOutcome.UNCHANGED_PRESENT).isPresent());
    }

    @Test
    public void messageForUnchangedAbsentIsPresent() {
        assertTrue(DetectConventionalConfigurationLocation.messageFor(ScanOutcome.UNCHANGED_ABSENT).isPresent());
    }
}
