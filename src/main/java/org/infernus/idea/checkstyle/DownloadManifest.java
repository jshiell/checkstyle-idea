package org.infernus.idea.checkstyle;

import org.infernus.idea.checkstyle.exception.CheckstyleDownloadException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


public class DownloadManifest {

    private static final String RESOURCE_NAME = "/checkstyle-download-manifest.properties";

    private final Properties properties;

    private DownloadManifest(@NotNull final Properties properties) {
        this.properties = properties;
    }

    @NotNull
    public static DownloadManifest fromClasspath() {
        try (InputStream is = DownloadManifest.class.getResourceAsStream(RESOURCE_NAME)) {
            if (is == null) {
                return new DownloadManifest(new Properties());
            }
            Properties props = new Properties();
            props.load(is);
            return new DownloadManifest(props);
        } catch (IOException e) {
            throw new CheckstyleDownloadException("Failed to load download manifest", e);
        }
    }

    @NotNull
    static DownloadManifest fromString(@NotNull final String content) {
        try {
            Properties props = new Properties();
            props.load(new StringReader(content));
            return new DownloadManifest(props);
        } catch (IOException e) {
            throw new CheckstyleDownloadException("Failed to parse download manifest", e);
        }
    }

    @NotNull
    public List<ManifestEntry> entriesFor(@NotNull final String version) {
        String value = properties.getProperty(version);
        if (value == null || value.isBlank()) {
            return List.of();
        }

        List<ManifestEntry> result = new ArrayList<>();
        for (String token : value.split("\\s*,\\s*")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                result.add(parseToken(trimmed));
            }
        }
        return result;
    }

    private static ManifestEntry parseToken(@NotNull final String token) {
        String[] parts = token.split(":", -1);
        if (parts.length != 5) {
            throw new CheckstyleDownloadException(
                    "Invalid manifest token (expected groupId:artifactId:version:classifier:sha256hex): " + token);
        }
        return new ManifestEntry(parts[0], parts[1], parts[2], parts[3], parts[4]);
    }
}
