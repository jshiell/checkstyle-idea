package org.infernus.idea.checkstyle;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactRepositoryCredentialsTest {

    @Test
    void ofReturnsPopulatedCredentialsWhenBothPresent() {
        Optional<ArtifactRepositoryCredentials> credentials =
                ArtifactRepositoryCredentials.of("user", "secret");

        assertTrue(credentials.isPresent());
        assertEquals("user", credentials.get().username());
        assertEquals("secret", credentials.get().password());
    }

    @Test
    void ofReturnsEmptyWhenUsernameBlank() {
        assertFalse(ArtifactRepositoryCredentials.of("", "secret").isPresent());
        assertFalse(ArtifactRepositoryCredentials.of("   ", "secret").isPresent());
        assertFalse(ArtifactRepositoryCredentials.of(null, "secret").isPresent());
    }

    @Test
    void ofReturnsEmptyWhenPasswordBlank() {
        assertFalse(ArtifactRepositoryCredentials.of("user", "").isPresent());
        assertFalse(ArtifactRepositoryCredentials.of("user", "   ").isPresent());
        assertFalse(ArtifactRepositoryCredentials.of("user", null).isPresent());
    }

    @Test
    void toStringDoesNotContainRawPassword() {
        ArtifactRepositoryCredentials credentials = new ArtifactRepositoryCredentials("user", "s3cr3t");

        String rendered = credentials.toString();

        assertFalse(rendered.contains("s3cr3t"));
        assertTrue(rendered.contains("user"));
    }
}
