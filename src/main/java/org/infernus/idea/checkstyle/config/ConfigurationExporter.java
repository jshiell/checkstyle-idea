package org.infernus.idea.checkstyle.config;

import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;

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
}
