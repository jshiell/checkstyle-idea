package org.infernus.idea.checkstyle.service;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;


public class StringConfigurationLocation
        extends ConfigurationLocation {

    private static final int MAX_TRIMMED_LENGTH = 100;

    private final String configurationXml;

    public StringConfigurationLocation(@NotNull final String configurationXml,
                                       @NotNull final Project project) {
        super(UUID.randomUUID().toString(), ConfigurationType.LOCAL_FILE, project);
        setDescription("In-memory String-based configuration: "
                + configurationXml.substring(0, Math.min(MAX_TRIMMED_LENGTH, configurationXml.length())) + " ...");
        this.configurationXml = configurationXml;
    }

    @NotNull
    @Override
    protected InputStream resolveFile(@NotNull final ClassLoader checkstyleClassLoader) {
        return new ByteArrayInputStream(configurationXml.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public StringConfigurationLocation clone() {
        return new StringConfigurationLocation(configurationXml, getProject());
    }
}
