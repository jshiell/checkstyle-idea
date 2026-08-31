package org.infernus.idea.checkstyle.gradle.tooling;

import org.gradle.api.Project;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService;

/**
 * Spike (Increment 0): proves that a class injected via IntelliJ's generated Gradle initscript can
 * reference a core Gradle plugin class ({@link CheckstyleExtension}) by static type, rather than
 * needing reflection. Will be replaced by the real model builder in Increment 3.
 */
public class CheckstyleGradleModelBuilder implements ModelBuilderService {

    @Override
    public boolean canBuild(final String modelName) {
        return CheckstyleGradleModel.class.getName().equals(modelName);
    }

    @Override
    public Object buildAll(final String modelName, final Project project) {
        final Object extension = project.getExtensions().findByName("checkstyle");
        if (!(extension instanceof CheckstyleExtension checkstyleExtension)) {
            return new CheckstyleGradleModelImpl(null);
        }

        final var configFile = checkstyleExtension.getConfigFile();
        return new CheckstyleGradleModelImpl(configFile != null ? configFile.getAbsolutePath() : null);
    }
}
