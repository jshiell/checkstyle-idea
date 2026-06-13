package org.infernus.idea.checkstyle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ManifestEntryTest {

    @TempDir
    Path m2Root;

    @Test
    void m2PathWithoutClassifier() {
        ManifestEntry entry = new ManifestEntry("com.puppycrawl.tools", "checkstyle", "10.26.1", "", "abc123");

        Path expected = m2Root
                .resolve("com/puppycrawl/tools/checkstyle/10.26.1/checkstyle-10.26.1.jar");

        assertEquals(expected, entry.m2Path(m2Root));
    }

    @Test
    void m2PathWithClassifier() {
        ManifestEntry entry = new ManifestEntry("org.xmlresolver", "xmlresolver", "5.3.3", "data", "def456");

        Path expected = m2Root
                .resolve("org/xmlresolver/xmlresolver/5.3.3/xmlresolver-5.3.3-data.jar");

        assertEquals(expected, entry.m2Path(m2Root));
    }

    @Test
    void mavenCentralUrlWithoutClassifier() {
        ManifestEntry entry = new ManifestEntry("com.puppycrawl.tools", "checkstyle", "10.26.1", "", "abc123");

        assertEquals(
                "https://repo1.maven.org/maven2/com/puppycrawl/tools/checkstyle/10.26.1/checkstyle-10.26.1.jar",
                entry.mavenCentralUrl());
    }

    @Test
    void mavenCentralUrlWithClassifier() {
        ManifestEntry entry = new ManifestEntry("org.xmlresolver", "xmlresolver", "5.3.3", "data", "def456");

        assertEquals(
                "https://repo1.maven.org/maven2/org/xmlresolver/xmlresolver/5.3.3/xmlresolver-5.3.3-data.jar",
                entry.mavenCentralUrl());
    }
}
