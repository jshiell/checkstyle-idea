package org.infernus.idea.checkstyle;


import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.junit.Assert;
import org.junit.Test;


public class VersionListReaderTest {
    @Test
    public void testNormalLoading() {
        VersionListReader underTest = new VersionListReader();
        Assert.assertNotNull(underTest.getSupportedVersions());
        Assert.assertTrue(underTest.getSupportedVersions().size() > 1);
        Assert.assertNotNull(underTest.getDefaultVersion());
        Assert.assertNotNull(underTest.getReplacementMap());
        Assert.assertTrue(underTest.getReplacementMap().size() > 1);
    }


    @Test
    public void testNonExistingFile() {
        try {
            new VersionListReader("non-existent.file");
            Assert.fail("expected exception was not thrown");
        } catch (CheckStylePluginException e) {
            // expected
            Assert.assertTrue(e.getMessage().startsWith("Internal error: Could not read internal configuration file"));
        }
    }


    @Test
    public void testSupportedVersionMustNotBeMapped() {
        try {
            new VersionListReader("checkstyle-idea.broken1.properties");
            Assert.fail("expected exception was not thrown");
        } catch (CheckStylePluginException e) {
            // expected
            Assert.assertEquals("Internal error: Property 'checkstyle.versions.map' contains "
                    + "invalid mapping '7.1 -> 7.2'. Checkstyle version 7.1 is in fact supported "
                    + "in configuration file 'checkstyle-idea.broken1.properties'", e.getMessage());
        }
    }


    @Test
    public void testTargetVersionMustExist() {
        try {
            new VersionListReader("checkstyle-idea.broken2.properties");
            Assert.fail("expected exception was not thrown");
        } catch (CheckStylePluginException e) {
            // expected
            Assert.assertEquals("Internal error: Property 'checkstyle.versions.map' contains "
                    + "invalid mapping '7.0 -> 7.1.1'. Target version 7.1.1 is not a supported "
                    + "version in configuration file 'checkstyle-idea.broken2.properties'", e.getMessage());
        }
    }
}
