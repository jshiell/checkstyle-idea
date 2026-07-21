package org.infernus.idea.checkstyle.ui;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.config.ApplicationConfigurationState.GlobalConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.UUID;


/**
 * A lightweight add/edit dialogue for global (IDE-wide) Checkstyle rule locations.
 * Does not require a project context; no Checkstyle validation is performed at entry time —
 * validation occurs when the location is first used by a project scan.
 */
public class GlobalLocationDialogue extends DialogWrapper {

    private static final int WIDTH = 500;
    private static final int MIN_HEIGHT = 160;

    // Only types that make sense globally (PROJECT_RELATIVE is intentionally excluded).
    private final ComboBox<ConfigurationType> typeCombo = new ComboBox<>(new ConfigurationType[]{
            ConfigurationType.LOCAL_FILE,
            ConfigurationType.HTTP_URL,
            ConfigurationType.INSECURE_HTTP_URL
    });
    private final JTextField locationField = new JTextField(40);
    private final JTextField descriptionField = new JTextField(40);

    @Nullable
    private final GlobalConfigurationLocation existingLocation;

    public GlobalLocationDialogue(@Nullable final GlobalConfigurationLocation existing) {
        super(true);
        this.existingLocation = existing;
        setTitle(existing == null
                ? CheckStyleBundle.message("config.file.add.title")
                : CheckStyleBundle.message("config.file.edit.title"));
        setSize(WIDTH, MIN_HEIGHT);
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        createGlobalConfigurationInputsIfNeeded();
        final JButton browseButton = new JButton(CheckStyleBundle.message("config.file.browse.text"));
        browseButton.setToolTipText(CheckStyleBundle.message("config.file.browse.tooltip"));
        browseButton.setEnabled(typeCombo.getSelectedItem() == ConfigurationType.LOCAL_FILE);
        
        browseButton.addActionListener(e -> {
            final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                    .withFileFilter(file -> "xml".equalsIgnoreCase(file.getExtension()));
            final VirtualFile chosen = FileChooser.chooseFile(descriptor, null, null);
            if (chosen != null) {
                locationField.setText(VfsUtilCore.virtualToIoFile(chosen).getAbsolutePath());
            }
        });
        typeCombo.addActionListener(e ->
                browseButton.setEnabled(typeCombo.getSelectedItem() == ConfigurationType.LOCAL_FILE));

        return globalSettingsPanelLayout();
    }

    private @NotNull JPanel globalSettingsPanelLayout() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final Insets insets = new Insets(4, 4, 4, 4);

        panel.add(new JLabel(CheckStyleBundle.message("config.global.location.type.label")),
                new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, insets, 0, 0));
        panel.add(typeCombo,
                new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, insets, 0, 0));

        panel.add(new JLabel(CheckStyleBundle.message("config.global.location.location.label")),
                new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, insets, 0, 0));
        panel.add(locationField,
                new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, insets, 0, 0));

        panel.add(new JLabel(CheckStyleBundle.message("config.global.location.description.label")),
                new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, insets, 0, 0));
        panel.add(descriptionField,
                new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, insets, 0, 0));
        return panel;
    }

    private void createGlobalConfigurationInputsIfNeeded() {
        if (existingLocation == null) {
            return;
        }
        final ConfigurationType type = ConfigurationType.parse(existingLocation.type);
        if (type != null) {
            typeCombo.setSelectedItem(type);
        }
        locationField.setText(Objects.requireNonNullElse(existingLocation.location, ""));
        descriptionField.setText(Objects.requireNonNullElse(existingLocation.description, ""));
    }

    @Override
    protected void doOKAction() {
        if (locationField.getText().isBlank()) {
            Messages.showErrorDialog(
                    getContentPanel(),
                    CheckStyleBundle.message("config.file.no-file"),
                    CheckStyleBundle.message("config.file.error.title"));
            return;
        }
        if (descriptionField.getText().isBlank()) {
            Messages.showErrorDialog(
                    getContentPanel(),
                    CheckStyleBundle.message("config.file.no-description"),
                    CheckStyleBundle.message("config.file.error.title"));
            return;
        }
        super.doOKAction();
    }

    /**
     * Returns the configured location DTO, or {@code null} if the dialogue was cancelled.
     *
     * @return the location or {@code null}.
     */
    @Nullable
    public GlobalConfigurationLocation getGlobalConfigurationLocation() {
        if (!isOK()) {
            return null;
        }
        final String id = (existingLocation != null && existingLocation.id != null)
                ? existingLocation.id
                : UUID.randomUUID().toString();
        final ConfigurationType type = (ConfigurationType) typeCombo.getSelectedItem();
        return new GlobalConfigurationLocation(
                id,
                type != null ? type.name() : ConfigurationType.LOCAL_FILE.name(),
                locationField.getText().trim(),
                descriptionField.getText().trim()
        );
    }

    @NotNull
    JTextField getLocationField() {
        return locationField;
    }

    @NotNull
    JTextField getDescriptionField() {
        return descriptionField;
    }
    
}
