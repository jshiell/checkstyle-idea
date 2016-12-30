package org.infernus.idea.checkstyle.service.entities;

import org.infernus.idea.checkstyle.service.Configurations;


public class InfernusConfigurationsObject
        implements HasInfernusConfigurations
{
    private final Configurations configurations;

    public InfernusConfigurationsObject(final Configurations pConfigurations) {
        configurations = pConfigurations;
    }

    @Override
    public Configurations getConfigurations() {
        return configurations;
    }
}
