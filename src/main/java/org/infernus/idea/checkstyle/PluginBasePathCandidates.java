package org.infernus.idea.checkstyle;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;


/**
 * Selects the first candidate plugin base path with the expected on-disk layout, evaluating
 * candidates lazily and in order. A candidate that resolves to a path but does not have the
 * expected layout is skipped rather than accepted, so a wrong-but-non-null guess cannot win over
 * a later, valid candidate.
 */
final class PluginBasePathCandidates {

    private PluginBasePathCandidates() {
    }

    @Nullable
    static String selectFirstValid(@NotNull final List<Supplier<String>> candidates,
                                    @NotNull final Predicate<String> hasExpectedLayout) {
        for (final Supplier<String> candidate : candidates) {
            final String path = candidate.get();
            if (path != null && hasExpectedLayout.test(path)) {
                return path;
            }
        }
        return null;
    }
}
