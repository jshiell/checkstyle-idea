package org.infernus.idea.checkstyle.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Pair<K, V>(K first, V second) {

    public static <K, V> Pair<K, V> of(@NotNull final K first, @Nullable final V second) {
        return new Pair<>(first, second);
    }

}
