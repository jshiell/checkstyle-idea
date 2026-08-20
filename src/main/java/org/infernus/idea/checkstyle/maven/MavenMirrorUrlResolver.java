package org.infernus.idea.checkstyle.maven;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.utils.MavenUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Resolves a Maven Central mirror URL from the user's Maven {@code settings.xml}, if the bundled Maven
 * plugin is available and a mirror applies. This is the only class in the plugin that references
 * {@code org.jetbrains.idea.maven.*} classes, so callers can safely guard against the optional dependency
 * being absent by catching any {@link Throwable} around calls into this class.
 */
public final class MavenMirrorUrlResolver {

    private static final String MAVEN_PLUGIN_ID = "org.jetbrains.idea.maven";
    private static final String CENTRAL_REPOSITORY_URL = "https://repo1.maven.org/maven2/";
    private static final String CENTRAL_REPOSITORY_ID = "central";

    private MavenMirrorUrlResolver() {
    }

    @NotNull
    public static Optional<String> resolveCentralMirror() {
        if (!isMavenPluginAvailable()) {
            return Optional.empty();
        }
        return resolveCentralMirrorFromSettings(MavenUtil.resolveUserSettingsPath(null, null));
    }

    @NotNull
    static Optional<String> resolveCentralMirrorFromSettings(@Nullable final Path settingsFile) {
        if (settingsFile == null || !Files.exists(settingsFile)) {
            return Optional.empty();
        }
        String mirroredUrl = MavenUtil.INSTANCE.getMirroredUrl(
                settingsFile, CENTRAL_REPOSITORY_URL, CENTRAL_REPOSITORY_ID);
        if (mirroredUrl != null && !mirroredUrl.equals(CENTRAL_REPOSITORY_URL)) {
            return Optional.of(mirroredUrl);
        }
        return Optional.empty();
    }

    private static boolean isMavenPluginAvailable() {
        PluginId pluginId = PluginId.getId(MAVEN_PLUGIN_ID);
        if (!PluginManagerCore.isPluginInstalled(pluginId)) {
            return false;
        }
        IdeaPluginDescriptor descriptor = PluginManagerCore.getPlugin(pluginId);
        return descriptor != null && descriptor.isEnabled();
    }
}
