package org.infernus.idea.checkstyle;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.util.ui.FormBuilder;
import org.infernus.idea.checkstyle.config.ApplicationConfigurationState;
import org.infernus.idea.checkstyle.config.ArtifactRepositoryCredentialsStore;
import org.infernus.idea.checkstyle.config.PasswordSafeArtifactRepositoryCredentialsStore;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;


/**
 * The application-level (IDE-wide) "configurable component" for CheckStyle plugin settings that are not
 * scoped to a single project, currently just the artifact download mirror override. Registered in
 * {@code plugin.xml} as an {@code applicationConfigurable} extension.
 */
public class CheckStyleApplicationConfigurable implements Configurable {

    private final ApplicationConfigurationState applicationConfigurationState;
    private final ArtifactRepositoryCredentialsStore credentialsStore;
    private JTextField artifactRepositoryBaseUrlOverrideField;
    private JTextField artifactRepositoryOverrideUsernameField;
    private JPasswordField artifactRepositoryOverridePasswordField;

    public CheckStyleApplicationConfigurable() {
        this(ApplicationManager.getApplication().getService(ApplicationConfigurationState.class),
                new PasswordSafeArtifactRepositoryCredentialsStore());
    }

    CheckStyleApplicationConfigurable(@NotNull final ApplicationConfigurationState applicationConfigurationState,
                                      @NotNull final ArtifactRepositoryCredentialsStore credentialsStore) {
        this.applicationConfigurationState = applicationConfigurationState;
        this.credentialsStore = credentialsStore;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return CheckStyleBundle.message("config.application.configuration-name");
    }

    @Override
    public JComponent createComponent() {
        artifactRepositoryBaseUrlOverrideField = new JTextField();
        artifactRepositoryBaseUrlOverrideField.setToolTipText(
                CheckStyleBundle.message("config.artefact-repository-base-url-override.tooltip"));

        artifactRepositoryOverrideUsernameField = new JTextField();
        artifactRepositoryOverrideUsernameField.setToolTipText(
                CheckStyleBundle.message("config.artefact-repository-override-username.tooltip"));

        artifactRepositoryOverridePasswordField = new JPasswordField();
        artifactRepositoryOverridePasswordField.setToolTipText(
                CheckStyleBundle.message("config.artefact-repository-override-password.tooltip"));

        reset();

        final JTextArea description = new JTextArea(CheckStyleBundle.message("config.artefact-repository-base-url-override.description"));
        description.setFont(UIManager.getFont("Label.font"));
        description.setEditable(false);
        description.setOpaque(false);
        description.setWrapStyleWord(true);
        description.setLineWrap(true);

        return FormBuilder.createFormBuilder()
                .addComponent(description)
                .addLabeledComponent(
                        CheckStyleBundle.message("config.artefact-repository-base-url-override.label.text"),
                        artifactRepositoryBaseUrlOverrideField)
                .addLabeledComponent(
                        CheckStyleBundle.message("config.artefact-repository-override-username.label.text"),
                        artifactRepositoryOverrideUsernameField)
                .addLabeledComponent(
                        CheckStyleBundle.message("config.artefact-repository-override-password.label.text"),
                        artifactRepositoryOverridePasswordField)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    @Override
    public boolean isModified() {
        if (!Objects.equals(
                normalise(artifactRepositoryBaseUrlOverrideField.getText()),
                normalise(applicationConfigurationState.getArtifactRepositoryBaseUrlOverride()))) {
            return true;
        }

        String typedUsername = emptyIfNull(normalise(artifactRepositoryOverrideUsernameField.getText()));
        String persistedUsername = emptyIfNull(applicationConfigurationState.getArtifactRepositoryOverrideUsername());
        if (!typedUsername.equals(persistedUsername)) {
            return true;
        }

        String typedPassword = new String(artifactRepositoryOverridePasswordField.getPassword());
        String storedPassword = persistedUsername.isBlank()
                ? "" : credentialsStore.getPassword(persistedUsername).orElse("");
        return !typedPassword.equals(storedPassword);
    }

    @Override
    public void apply() {
        String previousUsername = emptyIfNull(applicationConfigurationState.getArtifactRepositoryOverrideUsername());
        String newUsername = emptyIfNull(normalise(artifactRepositoryOverrideUsernameField.getText()));
        String typedPassword = new String(artifactRepositoryOverridePasswordField.getPassword());

        applicationConfigurationState.setArtifactRepositoryBaseUrlOverride(
                normalise(artifactRepositoryBaseUrlOverrideField.getText()));
        applicationConfigurationState.setArtifactRepositoryOverrideUsername(
                newUsername.isBlank() ? null : newUsername);

        if (!newUsername.equals(previousUsername) && !previousUsername.isBlank()) {
            credentialsStore.setPassword(previousUsername, "");
        }
        if (!newUsername.isBlank()) {
            credentialsStore.setPassword(newUsername, typedPassword);
        }
    }

    @Override
    public void reset() {
        artifactRepositoryBaseUrlOverrideField.setText(
                Objects.requireNonNullElse(applicationConfigurationState.getArtifactRepositoryBaseUrlOverride(), ""));

        String persistedUsername = emptyIfNull(applicationConfigurationState.getArtifactRepositoryOverrideUsername());
        artifactRepositoryOverrideUsernameField.setText(persistedUsername);
        artifactRepositoryOverridePasswordField.setText(
                persistedUsername.isBlank() ? "" : credentialsStore.getPassword(persistedUsername).orElse(""));
    }

    JTextField getArtifactRepositoryBaseUrlOverrideField() {
        return artifactRepositoryBaseUrlOverrideField;
    }

    JTextField getArtifactRepositoryOverrideUsernameField() {
        return artifactRepositoryOverrideUsernameField;
    }

    JPasswordField getArtifactRepositoryOverridePasswordField() {
        return artifactRepositoryOverridePasswordField;
    }

    @Nullable
    private static String normalise(@Nullable final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @NotNull
    private static String emptyIfNull(@Nullable final String value) {
        return value == null ? "" : value;
    }
}
