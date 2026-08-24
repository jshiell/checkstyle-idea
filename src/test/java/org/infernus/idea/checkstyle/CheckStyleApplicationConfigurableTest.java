package org.infernus.idea.checkstyle;

import org.infernus.idea.checkstyle.config.ApplicationConfigurationState;
import org.infernus.idea.checkstyle.config.ArtifactRepositoryCredentialsStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import java.util.HashMap;
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

    @Test
    void resetLoadsUsernameAndPasswordFromPersistedUsername() {
        applicationConfigurationState.setArtifactRepositoryOverrideUsername("jane");
        credentialsStore.setPassword("jane", "secret");

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
        credentialsStore.setPassword("alice", "secret");
        configurable.reset();

        assertFalse(configurable.isModified());
    }
}
