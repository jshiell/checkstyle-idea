package org.infernus.idea.checkstyle;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record ArtifactRepositoryLocation(@NotNull String baseUrl,
                                          @NotNull Optional<ArtifactRepositoryCredentials> credentials) {

    @Override
    public String toString() {
        return "ArtifactRepositoryLocation[baseUrl=" + baseUrl + ", credentials=" + credentials + "]";
    }
}
