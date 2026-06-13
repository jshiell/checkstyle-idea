package org.infernus.idea.checkstyle;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class DownloadManifestTest {

    @Test
    void entriesForParsesTokensCorrectly() {
        String manifestContent =
                "10.26.1 = com.puppycrawl.tools:checkstyle:10.26.1::abc123, " +
                "org.antlr:antlr4-runtime:4.13.2::def456, " +
                "org.xmlresolver:xmlresolver:5.3.3:data:789abc\n";

        DownloadManifest manifest = DownloadManifest.fromString(manifestContent);
        List<ManifestEntry> entries = manifest.entriesFor("10.26.1");

        assertEquals(3, entries.size());

        ManifestEntry first = entries.get(0);
        assertEquals("com.puppycrawl.tools", first.groupId());
        assertEquals("checkstyle", first.artifactId());
        assertEquals("10.26.1", first.version());
        assertEquals("", first.classifier());
        assertEquals("abc123", first.sha256hex());

        ManifestEntry third = entries.get(2);
        assertEquals("org.xmlresolver", third.groupId());
        assertEquals("xmlresolver", third.artifactId());
        assertEquals("5.3.3", third.version());
        assertEquals("data", third.classifier());
        assertEquals("789abc", third.sha256hex());
    }

    @Test
    void entriesForReturnsEmptyListForUnknownVersion() {
        DownloadManifest manifest = DownloadManifest.fromString("10.26.1 = com.puppycrawl.tools:checkstyle:10.26.1::abc123\n");

        List<ManifestEntry> entries = manifest.entriesFor("9.0");

        assertTrue(entries.isEmpty());
    }

    @Test
    void entriesForHandlesMultiVersionManifest() {
        String manifestContent =
                "10.1 = com.puppycrawl.tools:checkstyle:10.1::aaa111\n" +
                "10.26.1 = com.puppycrawl.tools:checkstyle:10.26.1::bbb222\n";

        DownloadManifest manifest = DownloadManifest.fromString(manifestContent);

        assertEquals(1, manifest.entriesFor("10.1").size());
        assertEquals("10.1", manifest.entriesFor("10.1").get(0).version());
        assertEquals(1, manifest.entriesFor("10.26.1").size());
    }
}
