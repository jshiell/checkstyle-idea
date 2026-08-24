package org.infernus.idea.checkstyle.config;

import com.intellij.credentialStore.CredentialAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ArtifactRepositoryCredentialsKey {

    static final String SERVICE_NAME = "CheckStyle-IDEA Artifact Repository Override";

    private ArtifactRepositoryCredentialsKey() {
    }

    @NotNull
    static CredentialAttributes forUsername(@Nullable final String username) {
        return new CredentialAttributes(SERVICE_NAME, blankToNull(username));
    }

    @Nullable
    private static String blankToNull(@Nullable final String username) {
        return username == null || username.isBlank() ? null : username;
    }
}
