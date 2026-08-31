package org.infernus.idea.checkstyle.gradle.tooling;

public class CheckstyleGradleModelImpl implements CheckstyleGradleModel {

    private final String configFilePath;

    public CheckstyleGradleModelImpl(final String configFilePath) {
        this.configFilePath = configFilePath;
    }

    @Override
    public String getConfigFilePath() {
        return configFilePath;
    }
}
