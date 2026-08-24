package org.infernus.idea.checkstyle.maven;

import com.intellij.openapi.diagnostic.Logger;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.infernus.idea.checkstyle.ArtifactRepositoryCredentials;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the {@code <server>} credentials for a Maven mirror already detected in {@code settings.xml},
 * decrypting the password with the same libraries and master-password scheme Maven itself uses. Only
 * resolves {@code ${env.NAME}} and {@code ${NAME}} (system property) placeholders in the server's
 * username/password - not full Maven property interpolation (no {@code ${settings.x}} self-references,
 * POM properties, or {@code ${maven.home}}).
 */
public final class MavenServerCredentialsResolver {

    private static final Logger LOG = Logger.getInstance(MavenServerCredentialsResolver.class);
    private static final String SETTINGS_SECURITY_SYSTEM_PROPERTY = "settings.security";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(env\\.)?([^}]+)}");

    private MavenServerCredentialsResolver() {
    }

    @NotNull
    public static Path defaultSettingsSecurityPath() {
        String override = System.getProperty(SETTINGS_SECURITY_SYSTEM_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        return Path.of(System.getProperty("user.home"), ".m2", "settings-security.xml");
    }

    @NotNull
    public static Optional<ArtifactRepositoryCredentials> resolveCredentialsForMirror(
            @NotNull final Path settingsFile,
            @NotNull final Path settingsSecurityFile,
            @NotNull final String mirrorUrl) {
        Optional<Settings> settings = readSettings(settingsFile);
        if (settings.isEmpty()) {
            return Optional.empty();
        }

        Mirror matchedMirror = findMirrorByUrl(settings.get().getMirrors(), mirrorUrl);
        if (matchedMirror == null) {
            return Optional.empty();
        }

        Server server = settings.get().getServer(matchedMirror.getId());
        if (server == null) {
            LOG.warn("Maven mirror '" + matchedMirror.getId()
                    + "' has no matching <server> entry in settings.xml; downloading unauthenticated");
            return Optional.empty();
        }

        String username = resolvePlaceholders(server.getUsername());
        String rawPassword = resolvePlaceholders(server.getPassword());
        String password = decrypt(rawPassword, settingsSecurityFile);
        return ArtifactRepositoryCredentials.of(username, password);
    }

    @NotNull
    private static Optional<Settings> readSettings(@NotNull final Path settingsFile) {
        if (!Files.exists(settingsFile)) {
            return Optional.empty();
        }
        try (Reader reader = Files.newBufferedReader(settingsFile)) {
            return Optional.of(new SettingsXpp3Reader().read(reader));
        } catch (IOException | XmlPullParserException e) {
            LOG.warn("Failed to parse Maven settings.xml at " + settingsFile, e);
            return Optional.empty();
        }
    }

    @Nullable
    private static Mirror findMirrorByUrl(@Nullable final List<Mirror> mirrors, @NotNull final String mirrorUrl) {
        if (mirrors == null) {
            return null;
        }
        String normalisedTarget = normaliseUrl(mirrorUrl);
        for (Mirror mirror : mirrors) {
            if (normaliseUrl(mirror.getUrl()).equals(normalisedTarget)) {
                return mirror;
            }
        }
        return null;
    }

    @NotNull
    private static String normaliseUrl(@Nullable final String url) {
        if (url == null) {
            return "";
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    @Nullable
    private static String resolvePlaceholders(@Nullable final String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            boolean isEnvVariable = matcher.group(1) != null;
            String name = matcher.group(2);
            String resolved = isEnvVariable ? System.getenv(name) : System.getProperty(name);
            matcher.appendReplacement(result, Matcher.quoteReplacement(resolved != null ? resolved : matcher.group()));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    @Nullable
    private static String decrypt(@Nullable final String password, @NotNull final Path settingsSecurityFile) {
        if (password == null || password.isBlank()) {
            return password;
        }
        try {
            DefaultSecDispatcher dispatcher = new DefaultSecDispatcher(new DefaultPlexusCipher());
            dispatcher.setConfigurationFile(settingsSecurityFile.toString());
            return dispatcher.decrypt(password);
        } catch (Throwable t) {
            LOG.warn("Failed to decrypt Maven server password using " + settingsSecurityFile, t);
            return null;
        }
    }
}
