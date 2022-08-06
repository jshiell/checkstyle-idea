package org.infernus.idea.checkstyle.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.util.ui.JBUI;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.util.Icons;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.intellij.util.ui.JBUI.emptyInsets;

/**
 * Provides module level configuration UI.
 */
public class CheckStyleModuleConfigPanel extends JPanel {

    private static final Insets ISOLATED_COMPONENT_INSETS = JBUI.insets(8);
    private final JRadioButton useProjectConfigurationRadio = new JRadioButton();
    private final JRadioButton useModuleConfigurationRadio = new JRadioButton();
    private final JRadioButton excludeRadio = new JRadioButton();
    private final ComboBox<ConfigurationLocation> configurationFilesCombo = new ComboBox<>();
    private final DefaultComboBoxModel<ConfigurationLocation> configurationFilesModel = new DefaultComboBoxModel<>();
    private final JLabel configurationFilesLabel = new JLabel();

    private List<ConfigurationLocation> configurationLocations = new ArrayList<>();
    private List<ConfigurationLocation> activeLocations = new ArrayList<>();
    private boolean excluded;

    /**
     * Create a new panel.
     */
    public CheckStyleModuleConfigPanel() {
        super(new BorderLayout());

        initialise();
    }

    private void initialise() {
        add(buildConfigurationPanel(), BorderLayout.CENTER);
    }

    private JPanel buildConfigurationPanel() {
        final JPanel configPanel = new JPanel(new GridBagLayout());

        final JLabel informationLabel = new JLabel(CheckStyleBundle.message("config.module.information"),
                Icons.icon("/general/information.png"), SwingConstants.LEFT);
        configPanel.add(informationLabel, new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, JBUI.insets(16, 8), 0, 0));

        useProjectConfigurationRadio.setText(CheckStyleBundle.message("config.module.project-configuration.text"));
        useProjectConfigurationRadio.setToolTipText(CheckStyleBundle.message("config.module.project-configuration.tooltip"));
        useProjectConfigurationRadio.addActionListener(new RadioListener());

        useModuleConfigurationRadio.setText(CheckStyleBundle.message("config.module.module-configuration.text"));
        useModuleConfigurationRadio.setToolTipText(CheckStyleBundle.message("config.module.module-configuration.tooltip"));
        useModuleConfigurationRadio.addActionListener(new RadioListener());

        excludeRadio.setText(CheckStyleBundle.message("config.module.exclude.text"));
        excludeRadio.setToolTipText(CheckStyleBundle.message("config.module.exclude.tooltip"));
        excludeRadio.addActionListener(new RadioListener());

        final ButtonGroup radioGroup = new ButtonGroup();
        radioGroup.add(useProjectConfigurationRadio);
        radioGroup.add(useModuleConfigurationRadio);
        radioGroup.add(excludeRadio);

        configPanel.add(useProjectConfigurationRadio, new GridBagConstraints(0, 1, 2, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, ISOLATED_COMPONENT_INSETS, 0, 0));
        configPanel.add(useModuleConfigurationRadio, new GridBagConstraints(0, 2, 2, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, ISOLATED_COMPONENT_INSETS, 0, 0));

        configurationFilesLabel.setText(CheckStyleBundle.message("config.module.module-file.text"));
        configPanel.add(configurationFilesLabel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, JBUI.insets(8, 32, 8, 8), 0, 0));

        configurationFilesCombo.setToolTipText(CheckStyleBundle.message("config.module.module-file.tooltip"));
        configurationFilesCombo.setModel(configurationFilesModel);
        configPanel.add(configurationFilesCombo, new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, ISOLATED_COMPONENT_INSETS, 0, 0));

        configPanel.add(excludeRadio, new GridBagConstraints(0, 4, 2, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, ISOLATED_COMPONENT_INSETS, 0, 0));

        configPanel.add(Box.createGlue(), new GridBagConstraints(0, 5, 2, 1, 1.0, 1.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, emptyInsets(), 0, 0));

        useProjectConfigurationRadio.setSelected(true);
        configurationFilesLabel.setEnabled(false);
        configurationFilesCombo.setEnabled(false);

        return configPanel;
    }

    private List<ConfigurationLocation> getConfigurationLocations() {
        final List<ConfigurationLocation> locations = new ArrayList<>();

        for (int i = 0; i < configurationFilesModel.getSize(); ++i) {
            locations.add(configurationFilesModel.getElementAt(i));
        }

        return Collections.unmodifiableList(locations);
    }

    public void setConfigurationLocations(final List<ConfigurationLocation> locations) {
        this.configurationLocations = Objects.requireNonNullElseGet(locations, ArrayList::new);

        configurationFilesModel.removeAllElements();

        if (locations != null && !locations.isEmpty()) {
            locations.forEach(configurationFilesModel::addElement);
            configurationFilesModel.setSelectedItem(locations.get(0));
        }

        useModuleConfigurationRadio.setEnabled(locations != null && !locations.isEmpty());
    }

    /**
     * Set the configuration to use, or null to use the project configuration.
     *
     * @param activeLocations the configuration, or null to use the project configuration.
     */
    public void setActiveLocations(final List<ConfigurationLocation> activeLocations) {
        this.activeLocations = Objects.requireNonNullElseGet(activeLocations, ArrayList::new);

        if (!activeLocations.isEmpty()) {
            configurationFilesCombo.setSelectedItem(activeLocations.get(0));
        } else if (configurationFilesModel.getSize() > 0) {
            configurationFilesCombo.setSelectedItem(configurationFilesModel.getElementAt(0));
        }

        if (!activeLocations.isEmpty()) {
            useModuleConfigurationRadio.setSelected(true);
        } else if (!excluded) {
            useProjectConfigurationRadio.setSelected(true);
        }

        new RadioListener().actionPerformed(null);
    }

    /**
     * Get the configuration to use, or null to use the project configuration.
     *
     * @return the configuration, or null to use the project configuration.
     */
    public ConfigurationLocation getActiveLocation() {
        if (useProjectConfigurationRadio.isSelected() || excludeRadio.isSelected()) {
            return null;
        }

        return (ConfigurationLocation) configurationFilesModel.getSelectedItem();
    }

    public void setExcluded(final boolean excluded) {
        this.excluded = excluded;

        if (excluded) {
            excludeRadio.setSelected(true);
        }
    }

    public boolean isExcluded() {
        return excludeRadio.isSelected();
    }

    /**
     * Have the contents been modified since being set?
     *
     * @return true if modified.
     */
    public boolean isModified() {
        return !activeLocations.contains(getActiveLocation())
                || !Objects.equals(configurationLocations, getConfigurationLocations())
                || excluded != isExcluded();
    }

    /**
     * Listener to update UI based on radio button selections.
     */
    private class RadioListener implements ActionListener {

        @Override
        public void actionPerformed(final ActionEvent e) {
            final boolean showModuleConfig = useModuleConfigurationRadio.isSelected();

            configurationFilesLabel.setEnabled(showModuleConfig);
            configurationFilesCombo.setEnabled(showModuleConfig);
        }
    }

}
