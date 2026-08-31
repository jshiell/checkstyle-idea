package org.infernus.idea.checkstyle.gradle.tooling;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.gradle.api.Project;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService;

/**
 * Reads a Gradle module's {@code checkstyle {}} extension configuration during sync, for
 * {@code GradleCheckstyleResolver} to pick up on the IDE side. Runs inside the target project's own
 * Gradle daemon via classpath injection, so it must never let an exception escape {@link #buildAll} —
 * one throw here becomes a user-visible sync failure on every Gradle project on the machine, not just
 * ones using this plugin.
 */
public class CheckstyleGradleModelBuilder implements ModelBuilderService {

    @Override
    public boolean canBuild(final String modelName) {
        return CheckstyleGradleModel.class.getName().equals(modelName);
    }

    @Override
    public Object buildAll(final String modelName, final Project project) {
        try {
            final Object extension = project.getExtensions().findByName("checkstyle");
            if (!(extension instanceof CheckstyleExtension checkstyleExtension)) {
                return null;
            }

            return new CheckstyleGradleModelImpl(
                    extractConfigFile(checkstyleExtension, project),
                    stringifyConfigProperties(checkstyleExtension),
                    checkstyleExtension.getToolVersion());
        } catch (final Exception e) {
            return null;
        }
    }

    private static String extractConfigFile(final CheckstyleExtension checkstyleExtension, final Project project) {
        final File configFile = checkstyleExtension.getConfigFile();
        if (configFile == null || !configFile.isFile()) {
            return null;
        }

        final Path buildDir = project.getLayout().getBuildDirectory().get().getAsFile().toPath();
        if (configFile.toPath().startsWith(buildDir)) {
            // A non-file-backed TextResource (checkstyle { config = resources.text.fromString(...) }
            // or fromArchiveEntry(...)) materialises a throwaway temp file under the build directory
            // so external tools can read it as a File; that must never be imported as though it were a
            // real, user-authored config file. A file-backed TextResource's getConfigFile() returns the
            // real file directly with no such side effect, so this heuristic only ever excludes the
            // synthetic case.
            return null;
        }

        return configFile.getAbsolutePath();
    }

    private static Map<String, String> stringifyConfigProperties(final CheckstyleExtension checkstyleExtension) {
        final Map<String, Object> configProperties = checkstyleExtension.getConfigProperties();
        if (configProperties == null) {
            return Map.of();
        }

        final Map<String, String> result = new LinkedHashMap<>();
        configProperties.forEach((key, value) -> result.put(key, String.valueOf(value)));
        return result;
    }
}
