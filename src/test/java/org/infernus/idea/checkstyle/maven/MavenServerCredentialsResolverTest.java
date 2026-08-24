package org.infernus.idea.checkstyle.maven;

import org.infernus.idea.checkstyle.ArtifactRepositoryCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenServerCredentialsResolverTest {

    private static final String MIRROR_URL = "https://mirror.corp.example.com/repository/maven-central/";

    @TempDir
    Path tempDir;

    private Path missingSettingsSecurityFile;

    @BeforeEach
    void setUp() {
        missingSettingsSecurityFile = tempDir.resolve("no-such-settings-security.xml");
    }

    @Test
    void resolvesPlaintextPasswordUnchanged() throws IOException {
        Path settingsFile = writeSettings(MIRROR_URL, "corporate-mirror", "jane", "plaintext-secret");

        Optional<ArtifactRepositoryCredentials> result = MavenServerCredentialsResolver.resolveCredentialsForMirror(
                settingsFile, missingSettingsSecurityFile, MIRROR_URL);

        assertEquals(Optional.of(new ArtifactRepositoryCredentials("jane", "plaintext-secret")), result);
    }

    @Test
    void resolvesEncryptedPasswordViaGenuineRoundTrip() throws Exception {
        String masterPassword = "test-master-password";
        String serverPassword = "s3cr3t-server-password";

        DefaultPlexusCipher cipher = new DefaultPlexusCipher();
        String encryptedMaster = cipher.encryptAndDecorate(masterPassword, "settings.security");
        String encryptedServerPassword = cipher.encryptAndDecorate(serverPassword, masterPassword);

        Path settingsSecurityFile = tempDir.resolve("settings-security.xml");
        Files.writeString(settingsSecurityFile, "<settingsSecurity><master>" + encryptedMaster + "</master></settingsSecurity>");

        Path settingsFile = writeSettings(MIRROR_URL, "corporate-mirror", "jane", encryptedServerPassword);

        Optional<ArtifactRepositoryCredentials> result = MavenServerCredentialsResolver.resolveCredentialsForMirror(
                settingsFile, settingsSecurityFile, MIRROR_URL);

        assertEquals(Optional.of(new ArtifactRepositoryCredentials("jane", serverPassword)), result);
    }

    @Test
    void returnsEmptyWhenNoMirrorMatches() throws IOException {
        Path settingsFile = writeSettings("https://other-mirror.example.com/repo/", "other-mirror", "jane", "secret");

        Optional<ArtifactRepositoryCredentials> result = MavenServerCredentialsResolver.resolveCredentialsForMirror(
                settingsFile, missingSettingsSecurityFile, MIRROR_URL);

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsEmptyWhenNoServerMatchesMirrorId() throws IOException {
        Path settingsFile = tempDir.resolve("settings.xml");
        Files.writeString(settingsFile, """
                <settings>
                  <mirrors>
                    <mirror>
                      <id>corporate-mirror</id>
                      <mirrorOf>*</mirrorOf>
                      <url>%s</url>
                    </mirror>
                  </mirrors>
                  <servers>
                    <server>
                      <id>some-other-server</id>
                      <username>jane</username>
                      <password>secret</password>
                    </server>
                  </servers>
                </settings>
                """.formatted(MIRROR_URL));

        Optional<ArtifactRepositoryCredentials> result = MavenServerCredentialsResolver.resolveCredentialsForMirror(
                settingsFile, missingSettingsSecurityFile, MIRROR_URL);

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsEmptyRatherThanThrowingWhenSettingsSecurityFileMissingForEncryptedPassword() throws IOException {
        Path settingsFile = writeSettings(MIRROR_URL, "corporate-mirror", "jane", "{encryptedLookingButUnreadable}");

        Optional<ArtifactRepositoryCredentials> result = MavenServerCredentialsResolver.resolveCredentialsForMirror(
                settingsFile, missingSettingsSecurityFile, MIRROR_URL);

        assertTrue(result.isEmpty());
    }

    @Test
    void trailingSlashOnlyUrlDifferenceStillMatches() throws IOException {
        Path settingsFile = writeSettings(MIRROR_URL, "corporate-mirror", "jane", "plaintext-secret");
        String urlWithoutTrailingSlash = MIRROR_URL.substring(0, MIRROR_URL.length() - 1);

        Optional<ArtifactRepositoryCredentials> result = MavenServerCredentialsResolver.resolveCredentialsForMirror(
                settingsFile, missingSettingsSecurityFile, urlWithoutTrailingSlash);

        assertEquals(Optional.of(new ArtifactRepositoryCredentials("jane", "plaintext-secret")), result);
    }

    @Test
    void resolvesSystemPropertyPlaceholderInUsername() throws IOException {
        Path settingsFile = writeSettings(MIRROR_URL, "corporate-mirror", "${NEXUS_USER}", "plaintext-secret");

        System.setProperty("NEXUS_USER", "jane-from-sysprop");
        try {
            Optional<ArtifactRepositoryCredentials> result = MavenServerCredentialsResolver.resolveCredentialsForMirror(
                    settingsFile, missingSettingsSecurityFile, MIRROR_URL);

            assertEquals(Optional.of(new ArtifactRepositoryCredentials("jane-from-sysprop", "plaintext-secret")), result);
        } finally {
            System.clearProperty("NEXUS_USER");
        }
    }

    @Test
    void resolvesEnvPlaceholderInUsernameUsingPathEnvVariable() throws IOException {
        String pathValue = System.getenv("PATH");
        assumeEnvVariableIsSet(pathValue);
        Path settingsFile = writeSettings(MIRROR_URL, "corporate-mirror", "${env.PATH}", "plaintext-secret");

        Optional<ArtifactRepositoryCredentials> result = MavenServerCredentialsResolver.resolveCredentialsForMirror(
                settingsFile, missingSettingsSecurityFile, MIRROR_URL);

        assertEquals(Optional.of(new ArtifactRepositoryCredentials(pathValue, "plaintext-secret")), result);
    }

    private static void assumeEnvVariableIsSet(final String value) {
        org.junit.jupiter.api.Assumptions.assumeTrue(value != null && !value.isBlank());
    }

    private Path writeSettings(final String mirrorUrl, final String mirrorId,
                               final String username, final String password) throws IOException {
        Path settingsFile = tempDir.resolve("settings.xml");
        Files.writeString(settingsFile, """
                <settings>
                  <mirrors>
                    <mirror>
                      <id>%s</id>
                      <mirrorOf>*</mirrorOf>
                      <url>%s</url>
                    </mirror>
                  </mirrors>
                  <servers>
                    <server>
                      <id>%s</id>
                      <username>%s</username>
                      <password>%s</password>
                    </server>
                  </servers>
                </settings>
                """.formatted(mirrorId, mirrorUrl, mirrorId, username, password));
        return settingsFile;
    }
}
