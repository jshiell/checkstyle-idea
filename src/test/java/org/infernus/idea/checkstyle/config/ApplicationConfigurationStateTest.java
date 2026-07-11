package org.infernus.idea.checkstyle.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

        ApplicationConfigurationState.ApplicationSettings persisted = original.getState();

        ApplicationConfigurationState reloaded = new ApplicationConfigurationState();
        reloaded.loadState(persisted);

        assertEquals("https://mirror.example.com/repo/", reloaded.getArtifactRepositoryBaseUrlOverride());
    }
}
