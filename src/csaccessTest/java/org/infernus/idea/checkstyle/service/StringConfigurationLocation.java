package org.infernus.idea.checkstyle.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.jetbrains.annotations.NotNull;


public class StringConfigurationLocation
        extends ConfigurationLocation
{
    private final String configurationXml;


    public StringConfigurationLocation(@NotNull final String pConfigurationXml) {
        super(ConfigurationType.LOCAL_FILE);
        setDescription("In-memory String-based configuration: " + //
                pConfigurationXml.substring(0, Math.min(100, pConfigurationXml.length())) + " ...");
        configurationXml = pConfigurationXml;
    }


    @NotNull
    @Override
    protected InputStream resolveFile() throws IOException {
        return new ByteArrayInputStream(configurationXml.getBytes(StandardCharsets.UTF_8));
    }


    @Override
    public StringConfigurationLocation clone() {
        return new StringConfigurationLocation(configurationXml);
    }
}
