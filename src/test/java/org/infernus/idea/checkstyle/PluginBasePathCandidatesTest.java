package org.infernus.idea.checkstyle;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


class PluginBasePathCandidatesTest {

    @Test
    void skipsANonNullCandidateWithTheWrongLayout() {
        final List<Supplier<String>> candidates = List.of(() -> "wrong-layout", () -> "right-layout");

        final String result = PluginBasePathCandidates.selectFirstValid(candidates, "right-layout"::equals);

        assertEquals("right-layout", result);
    }

    @Test
    void theFirstValidCandidateWins() {
        final List<Supplier<String>> candidates = List.of(() -> "descriptor-path", () -> "fallback-path");

        final String result = PluginBasePathCandidates.selectFirstValid(candidates, "descriptor-path"::equals);

        assertEquals("descriptor-path", result);
    }

    @Test
    void skipsNullCandidates() {
        final List<Supplier<String>> candidates = List.of(() -> null, () -> "valid-path");

        final String result = PluginBasePathCandidates.selectFirstValid(candidates, "valid-path"::equals);

        assertEquals("valid-path", result);
    }

    @Test
    void fallsThroughToNullWhenNoCandidateIsValid() {
        final List<Supplier<String>> candidates = List.of(() -> "a", () -> "b");

        final String result = PluginBasePathCandidates.selectFirstValid(candidates, layout -> false);

        assertNull(result);
    }

    @Test
    void fallsThroughToNullForAnEmptyCandidateList() {
        final String result = PluginBasePathCandidates.selectFirstValid(new ArrayList<>(), layout -> true);

        assertNull(result);
    }
}
