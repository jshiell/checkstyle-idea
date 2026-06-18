package org.infernus.idea.checkstyle;


import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class VersionListReaderTest {
    @Test
    public void testNormalLoading() {
        VersionListReader underTest = new VersionListReader();
        assertNotNull(underTest.getSupportedVersions());
        assertTrue(underTest.getSupportedVersions().size() > 1);
        assertNotNull(underTest.getDefaultVersion());
        assertNotNull(underTest.getReplacementMap());
        assertTrue(underTest.getReplacementMap().size() > 1);
    }


    @Test
    public void testNonExistingFile() {
        CheckStylePluginException e = assertThrows(CheckStylePluginException.class, () ->
                new VersionListReader("non-existent.file"));
        assertTrue(e.getMessage().startsWith("Internal error: Could not read internal configuration file"));
    }


    @Test
    public void testSupportedVersionMustNotBeMapped() {
        CheckStylePluginException e = assertThrows(CheckStylePluginException.class, () ->
                new VersionListReader("checkstyle-idea.broken1.properties"));
        assertEquals("Internal error: Property 'checkstyle.versions.map' contains "
                + "invalid mapping '7.1 -> 7.2'. Checkstyle version 7.1 is in fact supported "
                + "in configuration file 'checkstyle-idea.broken1.properties'", e.getMessage());
    }


    @Test
    public void testTargetVersionMustExist() {
        CheckStylePluginException e = assertThrows(CheckStylePluginException.class, () ->
                new VersionListReader("checkstyle-idea.broken2.properties"));
        assertEquals("Internal error: Property 'checkstyle.versions.map' contains "
                + "invalid mapping '7.0 -> 7.1.1'. Target version 7.1.1 is not a supported "
                + "version in configuration file 'checkstyle-idea.broken2.properties'", e.getMessage());
    }

    @Test
    public void testGetBundledVersionsReturnsConfiguredVersions() {
        VersionListReader underTest = new VersionListReader();
        assertNotNull(underTest.getBundledVersions());
        assertFalse(underTest.getBundledVersions().isEmpty());
        assertTrue(underTest.getBundledVersions().contains("10.0"));
        assertTrue(underTest.getBundledVersions().contains("13.5.0"));
    }

    @Test
    public void testIsBundledReturnsTrueForBundledVersion() {
        VersionListReader underTest = new VersionListReader();
        assertTrue(underTest.isBundled("10.0"));
        assertTrue(underTest.isBundled("13.5.0"));
    }

    @Test
    public void testIsBundledReturnsFalseForNonBundledVersion() {
        VersionListReader underTest = new VersionListReader();
        assertFalse(underTest.isBundled("10.4"));
        assertFalse(underTest.isBundled("11.0.1"));
    }

    @Test
    public void testBundledVersionsMustBeSubsetOfSupportedVersions() {
        CheckStylePluginException e = assertThrows(CheckStylePluginException.class, () ->
                new VersionListReader("checkstyle-idea.broken3.properties"));
        assertTrue(e.getMessage().contains("bundledVersions"));
        assertTrue(e.getMessage().contains("not a supported version"));
    }

    @Test
    public void isLatest_returnsTrueForLatestSentinel() {
        VersionListReader underTest = new VersionListReader();
        assertTrue(underTest.isLatest("latest"));
    }

    @Test
    public void isLatest_returnsFalseForConcreteVersion() {
        VersionListReader underTest = new VersionListReader();
        assertFalse(underTest.isLatest("10.0"));
    }
}
