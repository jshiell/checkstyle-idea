package org.infernus.idea.checkstyle.ui;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfoRt;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.util.ui.JBUI;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.config.ApplicationConfigurationState.GlobalConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.UUID;


/**
 * A lightweight add/edit dialogue for global (IDE-wide) Checkstyle rule locations.
 * Does not require a project context; no Checkstyle validation is performed at entry time —
 * validation occurs when the location is first used by a project scan.
 */
public class GlobalLocationDialogue extends DialogWrapper {

    private static final int WIDTH = 500;
    private static final int MIN_HEIGHT = 260;

    private enum LocationType {
        FILE, HTTP, CLASSPATH
    }

    private final JRadioButton fileLocationRadio = new JRadioButton(CheckStyleBundle.message("config.file.file.text"));
    private final JRadioButton urlLocationRadio = new JRadioButton(CheckStyleBundle.message("config.file.url.text"));
    private final JRadioButton classpathLocationRadio = new JRadioButton(CheckStyleBundle.message("config.file.classpath.text"));

    private final JLabel fileLocationLabel = new JLabel(CheckStyleBundle.message("config.file.file.label"));
    private final JLabel urlLocationLabel = new JLabel(CheckStyleBundle.message("config.file.url.label"));
    private final JLabel classpathLocationLabel = new JLabel(CheckStyleBundle.message("config.file.classpath.label"));

    private final JTextField fileLocationField = new JTextField(40);
    private final JTextField urlLocationField = new JTextField(40);
    private final JTextField classpathLocationField = new JTextField(40);

    private final JButton browseButton = new JButton(CheckStyleBundle.message("config.file.browse.text"));
    private final JCheckBox insecureHttpCheckbox = new JCheckBox(CheckStyleBundle.message("config.file.insecure-http.text"));
    private final JLabel classpathLocationReminderLabel = new JLabel(CheckStyleBundle.message("config.file.classpath.reminder"));

    private final JTextField descriptionField = new JTextField(40);
    private final ComboBox<String> scopeCombo = new ComboBox<>();

    private final JButton commitButton = new JButton();
    private final JButton previousButton = new JButton(CheckStyleBundle.message("config.file.previous.text"));

    @Nullable
    private final GlobalConfigurationLocation existingLocation;

    public GlobalLocationDialogue(@Nullable final GlobalConfigurationLocation existing) {
        super(true);
        this.existingLocation = existing;
        setTitle(existing == null
                ? CheckStyleBundle.message("config.file.add.title")
                : CheckStyleBundle.message("config.file.edit.title"));
        setSize(WIDTH, MIN_HEIGHT);
        initialiseRadios();
        initialiseWizardButtons();
        init();
    }

    private void initialiseWizardButtons() {
        commitButton.setText(CheckStyleBundle.message("config.file.okay.text"));
        commitButton.setToolTipText(CheckStyleBundle.message("config.file.okay.tooltip"));
        commitButton.addActionListener(this::onCommit);

        previousButton.setToolTipText(CheckStyleBundle.message("config.file.previous.tooltip"));
        previousButton.setEnabled(false);
    }

    @Override
    protected JComponent createSouthPanel() {
        final JPanel bottomPanel = new JPanel(new GridBagLayout());
        bottomPanel.setBorder(JBUI.Borders.empty(4, 8, 8, 8));
        final Insets insets = JBUI.insets(4);

        final JButton cancelButton = new JButton(getCancelAction());

        if (SystemInfoRt.isMac) {
            bottomPanel.add(cancelButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0));
            bottomPanel.add(Box.createHorizontalGlue(), new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        } else {
            bottomPanel.add(Box.createHorizontalGlue(), new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0));
            bottomPanel.add(cancelButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0));
        }
        bottomPanel.add(previousButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, insets, 0, 0));
        bottomPanel.add(commitButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, insets, 0, 0));

        return bottomPanel;
    }

    private void onCommit(final ActionEvent event) {
        commitButton.setEnabled(false);

        if (selectedLocationText().isBlank()) {
            showValidationError(CheckStyleBundle.message("config.file.no-file"));
            return;
        }
        if (descriptionField.getText().isBlank()) {
            showValidationError(CheckStyleBundle.message("config.file.no-description"));
            return;
        }
        close(OK_EXIT_CODE);
    }

    private void showValidationError(final String message) {
        Messages.showErrorDialog(getContentPanel(), message, CheckStyleBundle.message("config.file.error.title"));
        commitButton.setEnabled(true);
    }

    private void initialiseRadios() {
        browseButton.setToolTipText(CheckStyleBundle.message("config.file.browse.tooltip"));
        insecureHttpCheckbox.setToolTipText(CheckStyleBundle.message("config.file.insecure-http.tooltip"));

        final ButtonGroup locationGroup = new ButtonGroup();
        locationGroup.add(fileLocationRadio);
        locationGroup.add(urlLocationRadio);
        locationGroup.add(classpathLocationRadio);

        fileLocationRadio.addActionListener(e -> enabledLocation(LocationType.FILE));
        urlLocationRadio.addActionListener(e -> enabledLocation(LocationType.HTTP));
        classpathLocationRadio.addActionListener(e -> enabledLocation(LocationType.CLASSPATH));

        browseButton.addActionListener(e -> {
            final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                    .withFileFilter(file -> "xml".equalsIgnoreCase(file.getExtension()));
            final VirtualFile chosen = FileChooser.chooseFile(descriptor, null, null);
            if (chosen != null) {
                fileLocationField.setText(VfsUtilCore.virtualToIoFile(chosen).getAbsolutePath());
            }
        });

        fileLocationRadio.setSelected(true);
        enabledLocation(LocationType.FILE);
    }

    private void enabledLocation(final LocationType locationType) {
        fileLocationLabel.setEnabled(locationType == LocationType.FILE);
        fileLocationField.setEnabled(locationType == LocationType.FILE);
        browseButton.setEnabled(locationType == LocationType.FILE);

        urlLocationLabel.setEnabled(locationType == LocationType.HTTP);
        urlLocationField.setEnabled(locationType == LocationType.HTTP);
        insecureHttpCheckbox.setEnabled(locationType == LocationType.HTTP);

        classpathLocationLabel.setEnabled(locationType == LocationType.CLASSPATH);
        classpathLocationField.setEnabled(locationType == LocationType.CLASSPATH);
        classpathLocationReminderLabel.setEnabled(locationType == LocationType.CLASSPATH);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        initialiseScopeChoices();
        createGlobalConfigurationInputsIfNeeded();
        return globalSettingsPanelLayout();
    }

    private @NotNull JPanel globalSettingsPanelLayout() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final Insets insets = new Insets(4, 4, 4, 4);
        final Insets radioInsets = new Insets(8, 4, 4, 4);
        int row = 0;

        panel.add(new JLabel(CheckStyleBundle.message("config.file.description.text")),
                new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                        GridBagConstraints.NONE, insets, 0, 0));
        panel.add(descriptionField,
                new GridBagConstraints(1, row, 2, 1, 1.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, insets, 0, 0));
        row++;

        panel.add(fileLocationRadio,
                new GridBagConstraints(0, row, 3, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, insets, 0, 0));
        row++;

        final JPanel fileLocationRow = new JPanel(new BorderLayout(4, 0));
        fileLocationRow.add(fileLocationField, BorderLayout.CENTER);
        fileLocationRow.add(browseButton, BorderLayout.EAST);

        panel.add(fileLocationLabel,
                new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                        GridBagConstraints.NONE, insets, 0, 0));
        panel.add(fileLocationRow,
                new GridBagConstraints(1, row, 2, 1, 1.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, insets, 0, 0));
        row++;

        panel.add(urlLocationRadio,
                new GridBagConstraints(0, row, 3, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, radioInsets, 0, 0));
        row++;

        panel.add(urlLocationLabel,
                new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                        GridBagConstraints.NONE, insets, 0, 0));
        panel.add(urlLocationField,
                new GridBagConstraints(1, row, 2, 1, 1.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, insets, 0, 0));
        row++;

        panel.add(insecureHttpCheckbox,
                new GridBagConstraints(1, row, 2, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, insets, 0, 0));
        row++;

        panel.add(classpathLocationRadio,
                new GridBagConstraints(0, row, 3, 1, 0.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.NONE, radioInsets, 0, 0));
        row++;

        panel.add(classpathLocationLabel,
                new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                        GridBagConstraints.NONE, insets, 0, 0));
        panel.add(classpathLocationField,
                new GridBagConstraints(1, row, 2, 1, 1.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, insets, 0, 0));
        row++;

        panel.add(classpathLocationReminderLabel,
                new GridBagConstraints(1, row, 2, 1, 1.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, insets, 0, 0));
        row++;

        panel.add(Box.createVerticalGlue(),
                new GridBagConstraints(0, row, 3, 1, 0.0, 1.0, GridBagConstraints.WEST,
                        GridBagConstraints.VERTICAL, insets, 0, 0));
        row++;

        panel.add(new JLabel(CheckStyleBundle.message("config.file.scope.label")),
                new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                        GridBagConstraints.NONE, insets, 0, 0));
        panel.add(scopeCombo,
                new GridBagConstraints(1, row, 2, 1, 1.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, insets, 0, 0));

        return panel;
    }

    private void createGlobalConfigurationInputsIfNeeded() {
        if (existingLocation == null) {
            return;
        }
        final ConfigurationType type = ConfigurationType.parse(existingLocation.type);
        final String location = Objects.requireNonNullElse(existingLocation.location, "");
        if (type == ConfigurationType.HTTP_URL || type == ConfigurationType.INSECURE_HTTP_URL) {
            urlLocationRadio.setSelected(true);
            urlLocationField.setText(location);
            insecureHttpCheckbox.setSelected(type == ConfigurationType.INSECURE_HTTP_URL);
            enabledLocation(LocationType.HTTP);

        } else if (type == ConfigurationType.PLUGIN_CLASSPATH) {
            classpathLocationRadio.setSelected(true);
            classpathLocationField.setText(location);
            enabledLocation(LocationType.CLASSPATH);

        } else {
            fileLocationRadio.setSelected(true);
            fileLocationField.setText(location);
            enabledLocation(LocationType.FILE);
        }

        descriptionField.setText(Objects.requireNonNullElse(existingLocation.description, ""));
        final String scope = Objects.requireNonNullElse(existingLocation.scope, NamedScopeHelper.DEFAULT_SCOPE_ID);
        if (!hasScopeChoice(scope)) {
            scopeCombo.addItem(scope);
        }
        scopeCombo.setSelectedItem(scope);
    }

    private ConfigurationType selectedType() {
        if (urlLocationRadio.isSelected()) {
            return insecureHttpCheckbox.isSelected() ? ConfigurationType.INSECURE_HTTP_URL : ConfigurationType.HTTP_URL;
        } else if (classpathLocationRadio.isSelected()) {
            return ConfigurationType.PLUGIN_CLASSPATH;
        }
        return ConfigurationType.LOCAL_FILE;
    }

    private String selectedLocationText() {
        if (urlLocationRadio.isSelected()) {
            return urlLocationField.getText();
        } else if (classpathLocationRadio.isSelected()) {
            return classpathLocationField.getText();
        }
        return fileLocationField.getText();
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
        return new GlobalConfigurationLocation(
                id,
                selectedType().name(),
                selectedLocationText().trim(),
                descriptionField.getText().trim(),
                (String) scopeCombo.getSelectedItem()
        );
    }

    @NotNull
    JRadioButton fileLocationRadio() {
        return fileLocationRadio;
    }

    @NotNull
    JRadioButton urlLocationRadio() {
        return urlLocationRadio;
    }

    @NotNull
    JRadioButton classpathLocationRadio() {
        return classpathLocationRadio;
    }

    @NotNull
    JTextField fileLocationField() {
        return fileLocationField;
    }

    @NotNull
    JTextField urlLocationField() {
        return urlLocationField;
    }

    @NotNull
    JTextField classpathLocationField() {
        return classpathLocationField;
    }

    @NotNull
    JCheckBox insecureHttpCheckbox() {
        return insecureHttpCheckbox;
    }

    @NotNull
    JTextField descriptionField() {
        return descriptionField;
    }

    @NotNull
    ComboBox<String> scopeCombo() {
        return scopeCombo;
    }

    @NotNull
    JButton commitButton() {
        return commitButton;
    }

    @NotNull
    JButton previousButton() {
        return previousButton;
    }

    private void initialiseScopeChoices() {
        if (scopeCombo.getItemCount() > 0) {
            return;
        }

        final LinkedHashSet<String> scopeIds = new LinkedHashSet<>();
        scopeIds.add(NamedScopeHelper.DEFAULT_SCOPE_ID);
        final var projectManager = ProjectManager.getInstanceIfCreated();
        if (projectManager != null) {
            for (var project : projectManager.getOpenProjects()) {
                NamedScopeHelper.getAllScopes(project)
                        .map(NamedScope::getScopeId)
                        .forEach(scopeIds::add);
            }
        }

        scopeIds.forEach(scopeCombo::addItem);
        if (scopeCombo.getSelectedItem() == null) {
            scopeCombo.setSelectedItem(NamedScopeHelper.DEFAULT_SCOPE_ID);
        }
    }

    private boolean hasScopeChoice(@NotNull final String scopeId) {
        for (int i = 0; i < scopeCombo.getItemCount(); i++) {
            if (scopeId.equals(scopeCombo.getItemAt(i))) {
                return true;
            }
        }
        return false;
    }

}
