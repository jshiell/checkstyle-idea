package org.infernus.idea.checkstyle.config;

import com.intellij.credentialStore.CredentialAttributes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ArtifactRepositoryCredentialsKeyTest {

    @Test
    void keyUsesFixedServiceNameAndGivenUsername() {
        CredentialAttributes attributes = ArtifactRepositoryCredentialsKey.forUsername("jane");

        assertEquals("CheckStyle-IDEA Artifact Repository Override", attributes.getServiceName());
        assertEquals("jane", attributes.getUserName());
    }

    @Test
    void blankUsernameNormalisesToNullUserName() {
        assertNull(ArtifactRepositoryCredentialsKey.forUsername("").getUserName());
        assertNull(ArtifactRepositoryCredentialsKey.forUsername("   ").getUserName());
        assertNull(ArtifactRepositoryCredentialsKey.forUsername(null).getUserName());
    }

    @Test
    void differentUsernamesProduceDifferentKeys() {
        CredentialAttributes jane = ArtifactRepositoryCredentialsKey.forUsername("jane");
        CredentialAttributes john = ArtifactRepositoryCredentialsKey.forUsername("john");

        assertEquals(false, jane.equals(john));
    }
}
