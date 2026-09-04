package org.infernus.idea.checkstyle;

import org.infernus.idea.checkstyle.config.ApplicationConfigurationState;
import org.infernus.idea.checkstyle.config.ArtifactRepositoryCredentialsStore;
import org.infernus.idea.checkstyle.ui.GlobalLocationTableModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JCheckBox;
import javax.swing.JTextField;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class CheckStyleApplicationConfigurableTest {

    private static class FakeArtifactRepositoryCredentialsStore implements ArtifactRepositoryCredentialsStore {

        private final Map<String, String> passwords = new HashMap<>();

        @Override
        public Optional<String> getPassword(final String username) {
            return Optional.ofNullable(passwords.get(username));
        }

        @Override
        public void setPassword(final String username, final String password) {
            if (password == null || password.isEmpty()) {
                passwords.remove(username);
            } else {
                passwords.put(username, password);
            }
        }
    }

    private static class ThrowingOnUnexpectedUsernameCredentialsStore implements ArtifactRepositoryCredentialsStore {

        private final String expectedUsername;
        private final Map<String, String> passwords;

        ThrowingOnUnexpectedUsernameCredentialsStore(final String expectedUsername, final Map<String, String> passwords) {
            this.expectedUsername = expectedUsername;
            this.passwords = passwords;
        }

        @Override
        public Optional<String> getPassword(final String username) {
            if (!expectedUsername.equals(username)) {
                fail("Expected credentials lookup for persisted username '" + expectedUsername
                        + "' but got '" + username + "'");
            }
            return Optional.ofNullable(passwords.get(username));
        }

        @Override
        public void setPassword(final String username, final String password) {
            fail("setPassword should not be called from isModified()");
        }
    }

    private ApplicationConfigurationState applicationConfigurationState;
    private FakeArtifactRepositoryCredentialsStore credentialsStore;
    private CheckStyleApplicationConfigurable configurable;

    @BeforeEach
    void setUp() {
        applicationConfigurationState = new ApplicationConfigurationState();
        credentialsStore = new FakeArtifactRepositoryCredentialsStore();
        configurable = new CheckStyleApplicationConfigurable(applicationConfigurationState, credentialsStore);
        configurable.createComponent();
    }

    @Test
    void isNotModifiedInitially() {
        assertFalse(configurable.isModified());
    }

    @Test
    void isModifiedAfterUrlFieldChanges() {
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
        applicationConfiguration_state.setArtifactRepositoryBaseUrlOverride("https://mirror.example.com/repo/");
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

    @Test
    void resetLoadsUsernameAndPasswordFromPersistedUsername() {
        applicationConfigurationState.setArtifactRepositoryOverrideUsername("jane");
        credentials_store.setPassword("jane", "secret");

        configurable.reset();

        assertEquals("jane", configurable.getArtifactRepositoryOverrideUsernameField().getText());
        assertEquals("secret", new String(configurable.getArtifactRepositoryOverridePasswordField().getPassword()));
    }

    @Test
    void applyStoresUsernameAndPassword() {
        configurable.getArtifactRepositoryOverrideUsernameField().setText("jane");
        configurable.getArtifactRepositoryOverridePasswordField().setText("secret");

        configurable.apply();

        assertEquals("jane", applicationConfigurationState.getArtifactRepositoryOverrideUsername());
        assertEquals(Optional.of("secret"), credentialsStore.getPassword("jane"));
    }

    @Test
    void renamingUsernamePreservesPasswordUnderNewKeyAndErasesOldKey() {
        applicationConfigurationState.setArtifactRepositoryOverrideUsername("alice");
        credentialsStore.setPassword("alice", "secret");
        configurable.reset();

        configurable.getArtifactRepositoryOverrideUsernameField().setText("bob");
        configurable.apply();

        assertEquals("bob", applicationConfigurationState.getArtifactRepositoryOverrideUsername());
        assertEquals(Optional.of("secret"), credentialsStore.getPassword("bob"));
        assertEquals(Optional.empty(), credentialsStore.getPassword("alice"));
    }

    @Test
    void clearingBothFieldsErasesStoredEntry() {
        applicationConfigurationState.setArtifactRepositoryOverrideUsername("alice");
        credentialsStore.setPassword("alice", "secret");
        configurable.reset();

        configurable.getArtifactRepositoryOverrideUsernameField().setText("");
        configurable.getArtifactRepositoryOverridePasswordField().setText("");
        configurable.apply();

        assertNull(applicationConfigurationState.getArtifactRepositoryOverrideUsername());
        assertEquals(Optional.empty(), credentialsStore.getPassword("alice"));
    }

    @Test
    void editingOnlyUsernameDoesNotLookUpPasswordUnderTheNewlyTypedUsername() {
        applicationConfigurationState.setArtifactRepositoryOverrideUsername("alice");
        Map<String, String> passwords = new HashMap<>();
        passwords.put("alice", "secret");
        ThrowingOnUnexpectedUsernameCredentialsStore guardedStore =
                new ThrowingOnUnexpectedUsernameCredentialsStore("alice", passwords);
        CheckStyleApplicationConfigurable guardedConfigurable =
                new CheckStyleApplicationConfigurable(applicationConfigurationState, guardedStore);
        guardedConfigurable.createComponent();
        guardedConfigurable.reset();

        guardedConfigurable.getArtifactRepositoryOverrideUsernameField().setText("bob");

        assertTrue(guardedConfigurable.isModified());
    }

    @Test
    void isNotModifiedWhenUsernameAndPasswordUnchangedAfterReset() {
        applicationConfigurationState.setArtifactRepositoryOverrideUsername("alice");
        credentials_store.setPassword("alice", "secret");
        configurable.reset();

        assertFalse(configurable.isModified());
    }

    @Test
    void isModifiedWhenGlobalDefaultCheckboxChanges() {
        JCheckBox useGlobalByDefault = getPrivateField("useGlobalRulesByDefaultCheckbox", JCheckBox.class);

        useGlobalByDefault.setSelected(true);

        assertTrue(configurable.isModified());
    }

    @Test
    void applyPersistsGlobalLocationSettings() {
        GlobalLocationTableModel tableModel = getPrivateField("globalLocationTableModel", GlobalLocationTableModel.class);
        JCheckBox useGlobalByDefault = getPrivateField("useGlobalRulesByDefaultCheckbox", JCheckBox.class);

        ApplicationConfigurationState.GlobalConfigurationLocation location =
                new ApplicationConfigurationState.GlobalConfigurationLocation(
                        "glob-1", "LOCAL_FILE", "c:/rules.xml", "Global", "All");

        tableModel.setLocations(List.of(location), List.of("glob-1"));
        useGlobalByDefault.setSelected(true);

        configurable.apply();

        assertTrue(applicationConfigurationState.isUseGlobalRulesByDefault());
        assertEquals(List.of(location), applicationConfigurationState.getGlobalLocations());
        assertEquals(List.of("glob-1"), application_configuration_state.getActiveGlobalLocationIds());
        assertFalse(configurable.isModified());
    }

    @Test
    void activeIdOrderingDoesNotMarkSettingsModified() {
        ApplicationConfigurationState.GlobalConfigurationLocation one =
                new ApplicationConfigurationState.GlobalConfigurationLocation("id-1", "LOCAL_FILE", "c:/1.xml", "One", "All");
        ApplicationConfigurationState.GlobalConfigurationLocation two =
                new ApplicationConfigurationState.GlobalConfigurationLocation("id-2", "LOCAL_FILE", "c:/2.xml", "Two", "All");
        application_configuration_state.setGlobalLocations(List.of(one, two));
        application_configuration_state.setActiveGlobalLocationIds(List.of("id-1", "id-2"));

        configurable.reset();
        GlobalLocationTableModel tableModel = getPrivateField("globalLocationTableModel", GlobalLocationTableModel.class);
        tableModel.setValueAt(false, 0, 0);
        tableModel.setValueAt(true, 0, 0);

        assertFalse(configurable.isModified());
    }

    private <T> T getPrivateField(String fieldName, Class<T> type) {
        try {
            Field field = CheckStyleApplicationConfigurable.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return type.cast(field.get(configurable));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to read field: " + fieldName, e);
        }
    }
}
