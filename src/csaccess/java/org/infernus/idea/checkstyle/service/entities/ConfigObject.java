package org.infernus.idea.checkstyle.service.entities;

import com.puppycrawl.tools.checkstyle.api.Configuration;

public class ConfigObject
    implements HasConfig
{
    private final Configuration configuration;

    public ConfigObject(final Configuration pConfiguration) {
        configuration = pConfiguration;
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
