package org.infernus.idea.checkstyle.build;

import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;

import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.StringJoiner;


public class ManifestFormatter {

    private ManifestFormatter() {
    }

    public static String buildEntry(final Set<ResolvedArtifact> artifacts) {
        StringJoiner joiner = new StringJoiner(", ");
        for (ResolvedArtifact artifact : artifacts) {
            joiner.add(formatToken(artifact));
        }
        return joiner.toString();
    }

    private static String formatToken(final ResolvedArtifact artifact) {
        ModuleVersionIdentifier id = artifact.getModuleVersion().getId();
        String classifier = artifact.getClassifier() != null ? artifact.getClassifier() : "";
        String sha256 = sha256Hex(artifact);
        return id.getGroup() + ":" + id.getName() + ":" + id.getVersion() + ":" + classifier + ":" + sha256;
    }

    private static String sha256Hex(final ResolvedArtifact artifact) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(artifact.getFile().toPath());
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to compute SHA-256 for " + artifact.getFile(), e);
        }
    }
}
