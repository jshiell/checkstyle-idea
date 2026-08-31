package org.infernus.idea.checkstyle.gradle.tooling;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.gradle.api.Project;
import org.gradle.api.plugins.quality.Checkstyle;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService;

/**
 * Reads a Gradle module's Checkstyle configuration during sync, for {@code GradleCheckstyleResolver} to
 * pick up on the IDE side. Runs inside the target project's own Gradle daemon via classpath injection,
 * so it must never let an exception escape {@link #buildAll} — one throw here becomes a user-visible
 * sync failure on every Gradle project on the machine, not just ones using this plugin.
 *
 * <p>Covers two shapes: the {@code checkstyle {}} extension, and the "Android variant" from issue #439
 * — a raw {@code task checkstyle(type: Checkstyle) { ... }} declared without the extension being
 * usefully configured (AGP applies the extension transitively but leaves the real configuration on the
 * task). Precedence: a {@code configFile} that exists on disk wins over one that doesn't; among those
 * that exist, the extension wins over a raw task.
 */
public class CheckstyleGradleModelBuilder implements ModelBuilderService {

    @Override
    public boolean canBuild(final String modelName) {
        return CheckstyleGradleModel.class.getName().equals(modelName);
    }

    @Override
    public Object buildAll(final String modelName, final Project project) {
        try {
            return buildModel(project);
        } catch (final Exception e) {
            return null;
        }
    }

    private static CheckstyleGradleModel buildModel(final Project project) {
        final Object rawExtension = project.getExtensions().findByName("checkstyle");
        final CheckstyleExtension extension = rawExtension instanceof CheckstyleExtension checkstyleExtension
                ? checkstyleExtension : null;
        final boolean extensionPresent = extension != null;

        final String toolVersion = extension != null ? extension.getToolVersion() : null;

        if (extension != null) {
            final File configFile = existingConfigFile(extension.getConfigFile(), project);
            if (configFile != null) {
                return new CheckstyleGradleModelImpl(configFile.getAbsolutePath(),
                        stringifyConfigProperties(extension.getConfigProperties()), toolVersion);
            }
        }

        // Only realises every Checkstyle task (a configuration-avoidance regression on large builds) once
        // the extension has been ruled out, not unconditionally on every sync.
        for (final Checkstyle task : project.getTasks().withType(Checkstyle.class)) {
            final File configFile = existingConfigFile(task.getConfigFile(), project);
            if (configFile != null) {
                return new CheckstyleGradleModelImpl(configFile.getAbsolutePath(),
                        stringifyConfigProperties(task.getConfigProperties()), toolVersion);
            }
        }

        if (extensionPresent) {
            return new CheckstyleGradleModelImpl(null,
                    stringifyConfigProperties(extension.getConfigProperties()), toolVersion);
        }

        return null;
    }

    private static File existingConfigFile(final File configFile, final Project project) {
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

        return configFile;
    }

    private static Map<String, String> stringifyConfigProperties(final Map<String, Object> configProperties) {
        if (configProperties == null) {
            return Map.of();
        }

        final Map<String, String> result = new LinkedHashMap<>();
        configProperties.forEach((key, value) -> result.put(key, String.valueOf(value)));
        return result;
    }
}
