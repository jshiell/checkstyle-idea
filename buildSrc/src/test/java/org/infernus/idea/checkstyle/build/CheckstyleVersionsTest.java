package org.infernus.idea.checkstyle.build;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.SortedSet;

import static org.junit.jupiter.api.Assertions.*;


public class CheckstyleVersionsTest {

    private static final File PROP_FILE = new File("../src/main/resources/checkstyle-idea.properties");

    @Test
    public void getBundledVersionsReturnsConfiguredVersions() {
        CheckstyleVersions underTest = new CheckstyleVersions(PROP_FILE);
        SortedSet<String> bundled = underTest.getBundledVersions();
        assertNotNull(bundled);
        assertFalse(bundled.isEmpty());
        assertTrue(bundled.contains("10.0"));
        assertTrue(bundled.contains("13.6.0"));
    }

    @Test
    public void getBundledVersionsIsSubsetOfSupportedVersions() {
        CheckstyleVersions underTest = new CheckstyleVersions(PROP_FILE);
        assertTrue(underTest.getVersions().containsAll(underTest.getBundledVersions()));
    }
}
