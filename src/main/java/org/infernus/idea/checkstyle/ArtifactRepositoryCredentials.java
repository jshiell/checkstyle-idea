package org.infernus.idea.checkstyle;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record ArtifactRepositoryCredentials(@NotNull String username, @NotNull String password) {

    @NotNull
    public static Optional<ArtifactRepositoryCredentials> of(final String username, final String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new ArtifactRepositoryCredentials(username, password));
    }

    @Override
    public String toString() {
        return "ArtifactRepositoryCredentials[username=" + username + ", password=***]";
    }
}
