package org.infernus.idea.checkstyle.config;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class PasswordSafeArtifactRepositoryCredentialsStore implements ArtifactRepositoryCredentialsStore {

    @Override
    @NotNull
    public Optional<String> getPassword(@NotNull final String username) {
        CredentialAttributes attributes = ArtifactRepositoryCredentialsKey.forUsername(username);
        return Optional.ofNullable(PasswordSafe.getInstance().getPassword(attributes));
    }

    @Override
    public void setPassword(@NotNull final String username, @NotNull final String password) {
        CredentialAttributes attributes = ArtifactRepositoryCredentialsKey.forUsername(username);
        PasswordSafe.getInstance().setPassword(attributes, password.isEmpty() ? null : password);
    }
}
