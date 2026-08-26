package org.infernus.idea.checkstyle.config;

import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.util.Streams;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Locale;

public final class ConfigurationExporter {

    private static final String XML_SUFFIX = ".xml";
    private static final String FALLBACK_FILE_NAME = "checkstyle";

    private ConfigurationExporter() {
    }

    @NotNull
    public static String suggestedFileName(@NotNull final ConfigurationLocation location) {
        String name = location.getDescription();
        if (name.toLowerCase(Locale.ENGLISH).endsWith(XML_SUFFIX)) {
            name = name.substring(0, name.length() - XML_SUFFIX.length());
        }

        name = name.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");

        if (name.isEmpty()) {
            name = FALLBACK_FILE_NAME;
        }

        return name + XML_SUFFIX;
    }

    public static boolean hasConfiguredProperties(@NotNull final ConfigurationLocation location) {
        return !location.getProperties().isEmpty();
    }

    public static void export(@NotNull final ConfigurationLocation location,
                              @NotNull final ClassLoader checkstyleClassLoader,
                              @NotNull final File destination) throws IOException {
        final ConfigurationLocation clone = (ConfigurationLocation) location.clone();
        final byte[] content;
        try (InputStream resolved = clone.resolve(checkstyleClassLoader)) {
            content = Streams.readContentOf(resolved);
        }
        Files.write(destination.toPath(), content);
    }
}
