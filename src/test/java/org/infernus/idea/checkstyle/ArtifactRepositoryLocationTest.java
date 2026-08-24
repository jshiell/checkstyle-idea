package org.infernus.idea.checkstyle;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactRepositoryLocationTest {

    @Test
    void bundlesBaseUrlAndCredentials() {
        ArtifactRepositoryCredentials credentials = new ArtifactRepositoryCredentials("user", "secret");
        ArtifactRepositoryLocation location =
                new ArtifactRepositoryLocation("https://repo.example.com/", Optional.of(credentials));

        assertEquals("https://repo.example.com/", location.baseUrl());
        assertEquals(Optional.of(credentials), location.credentials());
    }

    @Test
    void toStringDoesNotContainRawPassword() {
        ArtifactRepositoryLocation location = new ArtifactRepositoryLocation(
                "https://repo.example.com/",
                Optional.of(new ArtifactRepositoryCredentials("user", "s3cr3t")));

        String rendered = location.toString();

        assertFalse(rendered.contains("s3cr3t"));
        assertTrue(rendered.contains("https://repo.example.com/"));
    }
}
