package org.infernus.idea.checkstyle.service.entities;

import com.puppycrawl.tools.checkstyle.api.Configuration;


public class CsConfigObject
    implements HasCsConfig
{
    private final Configuration configuration;

    public CsConfigObject(final Configuration pConfiguration) {
        configuration = pConfiguration;
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
