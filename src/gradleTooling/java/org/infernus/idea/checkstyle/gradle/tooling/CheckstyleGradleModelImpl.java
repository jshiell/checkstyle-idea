package org.infernus.idea.checkstyle.gradle.tooling;

import java.util.Map;

public class CheckstyleGradleModelImpl implements CheckstyleGradleModel {

    private final String configFile;
    private final Map<String, String> configProperties;
    private final String toolVersion;

    public CheckstyleGradleModelImpl(final String configFile,
                                      final Map<String, String> configProperties,
                                      final String toolVersion) {
        this.configFile = configFile;
        this.configProperties = configProperties;
        this.toolVersion = toolVersion;
    }

    @Override
    public String getConfigFile() {
        return configFile;
    }

    @Override
    public Map<String, String> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getToolVersion() {
        return toolVersion;
    }
}
