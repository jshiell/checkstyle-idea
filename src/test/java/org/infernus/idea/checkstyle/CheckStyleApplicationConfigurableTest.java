package org.infernus.idea.checkstyle;

import org.infernus.idea.checkstyle.config.ApplicationConfigurationState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckStyleApplicationConfigurableTest {

    private ApplicationConfigurationState applicationConfigurationState;
    private CheckStyleApplicationConfigurable configurable;

    @BeforeEach
    void setUp() {
        applicationConfigurationState = new ApplicationConfigurationState();
        configurable = new CheckStyleApplicationConfigurable(applicationConfigurationState);
        configurable.createComponent();
    }

    @Test
    void isNotModifiedInitially() {
        assertFalse(configurable.isModified());
    }

    @Test
    void isModifiedAfterFieldChanges() {
        configurable.getArtifactRepositoryBaseUrlOverrideField().setText("https://mirror.example.com/repo/");

        assertTrue(configurable.isModified());
    }

    @Test
    void applyPersistsFieldValueToState() {
        configurable.getArtifactRepositoryBaseUrlOverrideField().setText("https://mirror.example.com/repo/");

        configurable.apply();

        assertEquals("https://mirror.example.com/repo/", applicationConfigurationState.getArtifactRepositoryBaseUrlOverride());
        assertFalse(configurable.isModified());
    }

    @Test
    void applyWithBlankFieldClearsOverride() {
        applicationConfigurationState.setArtifactRepositoryBaseUrlOverride("https://mirror.example.com/repo/");
        configurable.reset();

        configurable.getArtifactRepositoryBaseUrlOverrideField().setText("   ");
        configurable.apply();

        assertNull(applicationConfigurationState.getArtifactRepositoryBaseUrlOverride());
    }

    @Test
    void resetLoadsFieldFromState() {
        applicationConfigurationState.setArtifactRepositoryBaseUrlOverride("https://mirror.example.com/repo/");

        configurable.reset();

        JTextField field = configurable.getArtifactRepositoryBaseUrlOverrideField();
        assertEquals("https://mirror.example.com/repo/", field.getText());
    }
}
