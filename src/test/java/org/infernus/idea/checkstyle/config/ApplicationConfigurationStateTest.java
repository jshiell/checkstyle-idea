package org.infernus.idea.checkstyle.config;

import org.infernus.idea.checkstyle.model.NamedScopeHelper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationConfigurationStateTest {

    @Test
    void defaultOverrideIsNull() {
        ApplicationConfigurationState state = new ApplicationConfigurationState();

        assertNull(state.getArtifactRepositoryBaseUrlOverride());
    }

    @Test
    void setterUpdatesGetter() {
        ApplicationConfigurationState state = new ApplicationConfigurationState();

        state.setArtifactRepositoryBaseUrlOverride("https://mirror.example.com/repo/");

        assertEquals("https://mirror.example.com/repo/", state.getArtifactRepositoryBaseUrlOverride());
    }

    @Test
    void getStateAndLoadStateRoundTrip() {
        ApplicationConfigurationState original = new ApplicationConfigurationState();
        original.setArtifactRepositoryBaseUrlOverride("https://mirror.example.com/repo/");
        original.setUseGlobalRulesByDefault(true);
        original.setGlobalLocations(List.of(new ApplicationConfigurationState.GlobalConfigurationLocation(
                "id-1", "LOCAL_FILE", "c:/rules.xml", "Rules", "All")));
        original.setActiveGlobalLocationIds(List.of("id-1"));

        ApplicationConfigurationState.ApplicationSettings persisted = original.getState();

        ApplicationConfigurationState reloaded = new ApplicationConfigurationState();
        reloaded.loadState(persisted);

        assertEquals("https://mirror.example.com/repo/", reloaded.getArtifactRepositoryBaseUrlOverride());
        assertTrue(reloaded.isUseGlobalRulesByDefault());
        assertEquals(1, reloaded.getGlobalLocations().size());
        assertEquals(List.of("id-1"), reloaded.getActiveGlobalLocationIds());
    }

    @Test
    void globalLocationsAreDefensivelyCopiedAndImmutable() {
        ApplicationConfigurationState state = new ApplicationConfigurationState();
        List<ApplicationConfigurationState.GlobalConfigurationLocation> input = new ArrayList<>();
        input.add(new ApplicationConfigurationState.GlobalConfigurationLocation(
                "id-1", "LOCAL_FILE", "c:/rules.xml", "Rules", "All"));

        state.setGlobalLocations(input);
        input.clear();

        assertEquals(1, state.getGlobalLocations().size());
        assertThrows(UnsupportedOperationException.class,
                () -> state.getGlobalLocations().add(new ApplicationConfigurationState.GlobalConfigurationLocation()));
    }

    @Test
    void activeGlobalIdsAreDefensivelyCopiedAndImmutable() {
        ApplicationConfigurationState state = new ApplicationConfigurationState();
        List<String> ids = new ArrayList<>(List.of("id-1"));

        state.setActiveGlobalLocationIds(ids);
        ids.clear();

        assertEquals(List.of("id-1"), state.getActiveGlobalLocationIds());
        assertThrows(UnsupportedOperationException.class,
                () -> state.getActiveGlobalLocationIds().add("id-2"));
    }

    @Test
    void globalConfigurationLocationUsesDefaultScopeWhenFourArgConstructorUsed() {
        ApplicationConfigurationState.GlobalConfigurationLocation dto =
                new ApplicationConfigurationState.GlobalConfigurationLocation(
                        "id-1", "LOCAL_FILE", "c:/rules.xml", "Rules");

        assertEquals(NamedScopeHelper.DEFAULT_SCOPE_ID, dto.scope);
    }

    @Test
    void globalConfigurationLocationEqualityIncludesScopeAndProperties() {
        ApplicationConfigurationState.GlobalConfigurationLocation left =
                new ApplicationConfigurationState.GlobalConfigurationLocation(
                        "id-1", "LOCAL_FILE", "c:/rules.xml", "Rules", "All");
        left.properties = Map.of("k", "v");

        ApplicationConfigurationState.GlobalConfigurationLocation right =
                new ApplicationConfigurationState.GlobalConfigurationLocation(
                        "id-1", "LOCAL_FILE", "c:/rules.xml", "Rules", "All");
        right.properties = Map.of("k", "v");

        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
    }

    @Test
    void defaultOverrideUsernameIsNull() {
        ApplicationConfigurationState state = new ApplicationConfigurationState();

        assertNull(state.getArtifactRepositoryOverrideUsername());
    }

    @Test
    void overrideUsernameSetterUpdatesGetter() {
        ApplicationConfigurationState state = new ApplicationConfigurationState();

        state.setArtifactRepositoryOverrideUsername("jane");

        assertEquals("jane", state.getArtifactRepositoryOverrideUsername());
    }

    @Test
    void overrideUsernameRoundTripsThroughGetStateAndLoadState() {
        ApplicationConfigurationState original = new ApplicationConfigurationState();
        original.setArtifactRepositoryOverrideUsername("jane");

        ApplicationConfigurationState.ApplicationSettings persisted = original.getState();

        ApplicationConfigurationState reloaded = new ApplicationConfigurationState();
        reloaded.loadState(persisted);

        assertEquals("jane", reloaded.getArtifactRepositoryOverrideUsername());
    }
}
