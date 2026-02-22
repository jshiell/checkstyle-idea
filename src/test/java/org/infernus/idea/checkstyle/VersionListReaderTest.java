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
}
