package org.infernus.idea.checkstyle.config;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface ArtifactRepositoryCredentialsStore {

    @NotNull
    Optional<String> getPassword(@NotNull String username);

    void setPassword(@NotNull String username, @NotNull String password);
}
