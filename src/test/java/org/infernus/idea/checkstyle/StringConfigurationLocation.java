package org.infernus.idea.checkstyle;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.jetbrains.annotations.NotNull;


public class StringConfigurationLocation
        extends ConfigurationLocation {
    private final String configurationXml;

    public StringConfigurationLocation(@NotNull final String configurationXml,
                                       @NotNull final Project project) {
        super(UUID.randomUUID().toString(), ConfigurationType.LOCAL_FILE, project);
        setDescription("In-memory String-based configuration: "
                + configurationXml.substring(0, Math.min(100, configurationXml.length())) + " ...");
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
