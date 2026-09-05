package org.infernus.idea.checkstyle.gradle;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.serialization.PropertyMapping;
import java.io.Serializable;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * What one Gradle module's Checkstyle configuration looked like at sync time, carried from
 * {@link GradleCheckstyleResolver} (Gradle-sync side) to {@link GradleCheckstyleDataService}
 * (project-import side) via the external-system data cache. Stored to disk by that cache between IDE
 * restarts, so this must stay {@link Serializable}.
 */
public final class CheckstyleGradleModuleData implements Serializable {

    public static final Key<CheckstyleGradleModuleData> KEY = Key.create(CheckstyleGradleModuleData.class, 100);

    private final String gradleProjectPath;
    private final String configFile;
    private final Map<String, String> configProperties;
    private final String toolVersion;

    @PropertyMapping({"gradleProjectPath", "configFile", "configProperties", "toolVersion"})
    public CheckstyleGradleModuleData(@NotNull final String gradleProjectPath,
                                       @Nullable final String configFile,
                                       @NotNull final Map<String, String> configProperties,
                                       @Nullable final String toolVersion) {
        this.gradleProjectPath = gradleProjectPath;
        this.configFile = configFile;
        this.configProperties = configProperties;
        this.toolVersion = toolVersion;
    }

    @NotNull
    public String getGradleProjectPath() {
        return gradleProjectPath;
    }

    @Nullable
    public String getConfigFile() {
        return configFile;
    }

    @NotNull
    public Map<String, String> getConfigProperties() {
        return configProperties;
    }

    @Nullable
    public String getToolVersion() {
        return toolVersion;
    }
}
