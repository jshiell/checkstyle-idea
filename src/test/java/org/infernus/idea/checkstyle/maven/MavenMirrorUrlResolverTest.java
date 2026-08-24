package org.infernus.idea.checkstyle.maven;

import org.infernus.idea.checkstyle.ArtifactRepositoryCredentials;
import org.infernus.idea.checkstyle.ArtifactRepositoryLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenMirrorUrlResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsMirrorUrlWhenWildcardMirrorConfigured() throws IOException {
        Path settingsFile = writeSettings("""
                <settings>
                  <mirrors>
                    <mirror>
                      <id>corporate-mirror</id>
                      <mirrorOf>*</mirrorOf>
                      <url>https://mirror.corp.example.com/repository/maven-central/</url>
                    </mirror>
                  </mirrors>
                </settings>
                """);

        Optional<String> result = MavenMirrorUrlResolver.resolveCentralMirrorFromSettings(settingsFile);

        assertEquals(Optional.of("https://mirror.corp.example.com/repository/maven-central/"), result);
    }

    @Test
    void returnsEmptyWhenSettingsFileDoesNotExist() {
        Path missing = tempDir.resolve("does-not-exist.xml");

        Optional<String> result = MavenMirrorUrlResolver.resolveCentralMirrorFromSettings(missing);

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsEmptyWhenNoMirrorsConfigured() throws IOException {
        Path settingsFile = writeSettings("""
                <settings>
                </settings>
                """);

        Optional<String> result = MavenMirrorUrlResolver.resolveCentralMirrorFromSettings(settingsFile);

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsEmptyWhenMirrorDoesNotApplyToCentral() throws IOException {
        Path settingsFile = writeSettings("""
                <settings>
                  <mirrors>
                    <mirror>
                      <id>other-mirror</id>
                      <mirrorOf>some-other-repo</mirrorOf>
                      <url>https://mirror.corp.example.com/repository/other/</url>
                    </mirror>
                  </mirrors>
                </settings>
                """);

        Optional<String> result = MavenMirrorUrlResolver.resolveCentralMirrorFromSettings(settingsFile);

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsEmptyWhenMirrorUrlMatchesCentral() throws IOException {
        Path settingsFile = writeSettings("""
                <settings>
                  <mirrors>
                    <mirror>
                      <id>central-mirror</id>
                      <mirrorOf>*</mirrorOf>
                      <url>https://repo1.maven.org/maven2/</url>
                    </mirror>
                  </mirrors>
                </settings>
                """);

        Optional<String> result = MavenMirrorUrlResolver.resolveCentralMirrorFromSettings(settingsFile);

        assertTrue(result.isEmpty());
    }

    private Path writeSettings(final String xml) throws IOException {
        Path settingsFile = tempDir.resolve("settings.xml");
        Files.writeString(settingsFile, xml);
        return settingsFile;
    }

    @Test
    void resolveCentralMirrorWithCredentialsFromSettingsPairsUrlAndCredentials() throws IOException {
        Path settingsFile = writeSettings("""
                <settings>
                  <mirrors>
                    <mirror>
                      <id>corporate-mirror</id>
                      <mirrorOf>*</mirrorOf>
                      <url>https://mirror.corp.example.com/repository/maven-central/</url>
                    </mirror>
                  </mirrors>
                  <servers>
                    <server>
                      <id>corporate-mirror</id>
                      <username>jane</username>
                      <password>plaintext-secret</password>
                    </server>
                  </servers>
                </settings>
                """);
        Path missingSettingsSecurityFile = tempDir.resolve("no-such-settings-security.xml");

        Optional<ArtifactRepositoryLocation> result = MavenMirrorUrlResolver
                .resolveCentralMirrorWithCredentialsFromSettings(settingsFile, missingSettingsSecurityFile);

        assertEquals(Optional.of(new ArtifactRepositoryLocation(
                "https://mirror.corp.example.com/repository/maven-central/",
                Optional.of(new ArtifactRepositoryCredentials("jane", "plaintext-secret")))), result);
    }

    @Test
    void resolveCentralMirrorWithCredentialsFromSettingsReturnsUrlWithNoCredentialsWhenNoServerMatches() throws IOException {
        Path settingsFile = writeSettings("""
                <settings>
                  <mirrors>
                    <mirror>
                      <id>corporate-mirror</id>
                      <mirrorOf>*</mirrorOf>
                      <url>https://mirror.corp.example.com/repository/maven-central/</url>
                    </mirror>
                  </mirrors>
                </settings>
                """);
        Path missingSettingsSecurityFile = tempDir.resolve("no-such-settings-security.xml");

        Optional<ArtifactRepositoryLocation> result = MavenMirrorUrlResolver
                .resolveCentralMirrorWithCredentialsFromSettings(settingsFile, missingSettingsSecurityFile);

        assertEquals(Optional.of(new ArtifactRepositoryLocation(
                "https://mirror.corp.example.com/repository/maven-central/", Optional.empty())), result);
    }

    @Test
    void resolveCentralMirrorWithCredentialsFromSettingsReturnsEmptyWhenNoMirrorApplies() throws IOException {
        Path settingsFile = writeSettings("""
                <settings>
                </settings>
                """);
        Path missingSettingsSecurityFile = tempDir.resolve("no-such-settings-security.xml");

        Optional<ArtifactRepositoryLocation> result = MavenMirrorUrlResolver
                .resolveCentralMirrorWithCredentialsFromSettings(settingsFile, missingSettingsSecurityFile);

        assertTrue(result.isEmpty());
    }
}
