package org.infernus.idea.checkstyle.build;

import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedModuleVersion;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


public class ManifestFormatterTest {

    @TempDir
    Path tempDir;

    @Test
    void buildEntryFormatsTokensWithoutClassifier() throws Exception {
        File jar = createJar("checkstyle.jar", new byte[]{1, 2, 3});
        ResolvedArtifact artifact = stubArtifact("com.puppycrawl.tools", "checkstyle", "10.26.1", null, jar);

        String entry = ManifestFormatter.buildEntry(Set.of(artifact));

        assertTrue(entry.contains("com.puppycrawl.tools:checkstyle:10.26.1::"), entry);
    }

    @Test
    void buildEntryFormatsTokensWithClassifier() throws Exception {
        File jar = createJar("xmlresolver-data.jar", new byte[]{4, 5, 6});
        ResolvedArtifact artifact = stubArtifact("org.xmlresolver", "xmlresolver", "5.3.3", "data", jar);

        String entry = ManifestFormatter.buildEntry(Set.of(artifact));

        assertTrue(entry.contains("org.xmlresolver:xmlresolver:5.3.3:data:"), entry);
    }

    @Test
    void buildEntryIncludesSha256() throws Exception {
        byte[] content = new byte[]{1, 2, 3};
        File jar = createJar("checkstyle.jar", content);
        ResolvedArtifact artifact = stubArtifact("com.puppycrawl.tools", "checkstyle", "10.26.1", null, jar);

        String entry = ManifestFormatter.buildEntry(Set.of(artifact));

        // token format: groupId:artifactId:version:classifier:sha256
        String token = entry.trim();
        String[] parts = token.split(":");
        assertEquals(5, parts.length, "token should have 5 colon-separated parts: " + token);
        assertFalse(parts[4].isBlank(), "sha256 should not be blank");
    }

    @Test
    void buildEntryWithMultipleArtifactsJoinsWithComma() throws Exception {
        File jar1 = createJar("checkstyle.jar", new byte[]{1});
        File jar2 = createJar("antlr.jar", new byte[]{2});
        Set<ResolvedArtifact> artifacts = new LinkedHashSet<>();
        artifacts.add(stubArtifact("com.puppycrawl.tools", "checkstyle", "10.26.1", null, jar1));
        artifacts.add(stubArtifact("org.antlr", "antlr4-runtime", "4.13.2", null, jar2));

        String entry = ManifestFormatter.buildEntry(artifacts);

        assertTrue(entry.contains(", "), "multiple artifacts should be comma-separated: " + entry);
    }

    private File createJar(final String name, final byte[] content) throws Exception {
        File file = tempDir.resolve(name).toFile();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content);
        }
        return file;
    }

    private static ResolvedArtifact stubArtifact(final String group,
                                                  final String name,
                                                  final String version,
                                                  final String classifier,
                                                  final File file) {
        return new ResolvedArtifact() {
            @Override public String getName() { return name; }
            @Override public String getType() { return "jar"; }
            @Override public String getExtension() { return "jar"; }
            @Override public String getClassifier() { return classifier; }
            @Override public File getFile() { return file; }
            @Override public ResolvedModuleVersion getModuleVersion() {
                return () -> new org.gradle.api.artifacts.ModuleVersionIdentifier() {
                    @Override public String getGroup() { return group; }
                    @Override public String getName() { return name; }
                    @Override public String getVersion() { return version; }
                    @Override public org.gradle.api.artifacts.ModuleIdentifier getModule() {
                        return new org.gradle.api.artifacts.ModuleIdentifier() {
                            @Override public String getGroup() { return group; }
                            @Override public String getName() { return name; }
                        };
                    }
                };
            }
            @Override public ComponentArtifactIdentifier getId() { return null; }
        };
    }
}
