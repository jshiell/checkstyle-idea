package org.infernus.idea.checkstyle;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.util.ui.FormBuilder;
import org.infernus.idea.checkstyle.config.ApplicationConfigurationState;
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
    private JTextField artifactRepositoryBaseUrlOverrideField;

    public CheckStyleApplicationConfigurable() {
        this(ApplicationManager.getApplication().getService(ApplicationConfigurationState.class));
    }

    CheckStyleApplicationConfigurable(@NotNull final ApplicationConfigurationState applicationConfigurationState) {
        this.applicationConfigurationState = applicationConfigurationState;
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
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    @Override
    public boolean isModified() {
        return !Objects.equals(
                normalise(artifactRepositoryBaseUrlOverrideField.getText()),
                normalise(applicationConfigurationState.getArtifactRepositoryBaseUrlOverride()));
    }

    @Override
    public void apply() {
        applicationConfigurationState.setArtifactRepositoryBaseUrlOverride(
                normalise(artifactRepositoryBaseUrlOverrideField.getText()));
    }

    @Override
    public void reset() {
        artifactRepositoryBaseUrlOverrideField.setText(
                Objects.requireNonNullElse(applicationConfigurationState.getArtifactRepositoryBaseUrlOverride(), ""));
    }

    JTextField getArtifactRepositoryBaseUrlOverrideField() {
        return artifactRepositoryBaseUrlOverrideField;
    }

    @Nullable
    private static String normalise(@Nullable final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
