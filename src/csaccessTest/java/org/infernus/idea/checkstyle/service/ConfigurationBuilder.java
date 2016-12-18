package org.infernus.idea.checkstyle.service;

import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.api.Configuration;

public class ConfigurationBuilder {

    private final DefaultConfiguration configuration;

    private ConfigurationBuilder(final DefaultConfiguration configuration) {
        this.configuration = configuration;
    }

    public static ConfigurationBuilder checker() {
        return new ConfigurationBuilder(new DefaultConfiguration("Checker"));
    }

    public static ConfigurationBuilder config(final String name) {
        return new ConfigurationBuilder(new DefaultConfiguration(name));
    }

    public ConfigurationBuilder withChild(final ConfigurationBuilder child) {
        configuration.addChild(child.build());
        return this;
    }

    public ConfigurationBuilder withAttribute(final String name, final String value) {
        configuration.addAttribute(name, value);
        return this;
    }

    public ConfigurationBuilder withMessage(final String key, final String value) {
        configuration.addMessage(key, value);
        return this;
    }

    public Configuration build() {
        return configuration;
    }

}
