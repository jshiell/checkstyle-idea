package org.infernus.idea.checkstyle.ui;

import org.apache.commons.lang.ObjectUtils;
import org.infernus.idea.checkstyle.CheckStyleConstants;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Provides module level configuration UI.
 */
public class CheckStyleModuleConfigPanel extends JPanel {

    private final JRadioButton useProjectConfigurationRadio = new JRadioButton();
    private final JRadioButton useModuleConfigurationRadio = new JRadioButton();
    private final JComboBox configurationFilesCombo = new JComboBox();
    private final DefaultComboBoxModel configurationFilesModel = new DefaultComboBoxModel();
    private final JLabel configurationFilesLabel = new JLabel();

    private List<ConfigurationLocation> configurationLocations;
    private ConfigurationLocation activeLocation;

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
        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);

        final JPanel configPanel = new JPanel(new GridBagLayout());

        final JLabel informationLabel = new JLabel(resources.getString("config.module.information"));
        configPanel.add(informationLabel, new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(8, 8, 16, 8), 0, 0));

        useProjectConfigurationRadio.setText(resources.getString("config.module.project-configuration.text"));
        useProjectConfigurationRadio.setToolTipText(resources.getString("config.module.project-configuration.tooltip"));
        useProjectConfigurationRadio.addActionListener(new RadioListener());

        useModuleConfigurationRadio.setText(resources.getString("config.module.module-configuration.text"));
        useModuleConfigurationRadio.setToolTipText(resources.getString("config.module.module-configuration.tooltip"));
        useModuleConfigurationRadio.addActionListener(new RadioListener());

        final ButtonGroup radioGroup = new ButtonGroup();
        radioGroup.add(useProjectConfigurationRadio);
        radioGroup.add(useModuleConfigurationRadio);

        configPanel.add(useProjectConfigurationRadio, new GridBagConstraints(0, 1, 2, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(8, 8, 8, 8), 0, 0));
        configPanel.add(useModuleConfigurationRadio, new GridBagConstraints(0, 2, 2, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(8, 8, 8, 8), 0, 0));

        configurationFilesLabel.setText(resources.getString("config.module.module-file.text"));
        configPanel.add(configurationFilesLabel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(8, 32, 8, 8), 0, 0));

        configurationFilesCombo.setToolTipText(resources.getString("config.module.module-file.tooltip"));
        configurationFilesCombo.setModel(configurationFilesModel);
        configPanel.add(configurationFilesCombo, new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(8, 8, 8, 8), 0, 0));

        configPanel.add(Box.createGlue(), new GridBagConstraints(0, 4, 2, 1, 1.0, 1.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        useProjectConfigurationRadio.setSelected(true);
        configurationFilesLabel.setEnabled(false);
        configurationFilesCombo.setEnabled(false);

        return configPanel;
    }

    private List<ConfigurationLocation> getConfigurationLocations() {
        final List<ConfigurationLocation> locations = new ArrayList<ConfigurationLocation>();

        for (int i = 0; i < configurationFilesModel.getSize(); ++i) {
            locations.add((ConfigurationLocation) configurationFilesModel.getElementAt(i));
        }

        return Collections.unmodifiableList(locations);
    }

    public void setConfigurationLocations(final List<ConfigurationLocation> locations) {
        this.configurationLocations = locations;

        configurationFilesModel.removeAllElements();

        if (locations != null && locations.size() > 0) {
            for (final ConfigurationLocation location : locations) {
                configurationFilesModel.addElement(location);
            }

            configurationFilesModel.setSelectedItem(locations.get(0));
        }

        useModuleConfigurationRadio.setEnabled(locations != null && locations.size() > 0);
    }

    /**
     * Set the configuration to use, or null to use the project configuration.
     *
     * @param activeLocation the configuration, or null to use the project configuration.
     */
    public void setActiveLocation(final ConfigurationLocation activeLocation) {
        this.activeLocation = activeLocation;

        if (activeLocation != null) {
            configurationFilesCombo.setSelectedItem(activeLocation);
        } else if (configurationFilesModel.getSize() > 0) {
            configurationFilesCombo.setSelectedItem(configurationFilesModel.getElementAt(0));
        }

        if (activeLocation != null) {
            useModuleConfigurationRadio.setSelected(true);
        } else {
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
        if (useProjectConfigurationRadio.isSelected()) {
            return null;
        }

        return (ConfigurationLocation) configurationFilesModel.getSelectedItem();
    }

    /**
     * Have the contents been modified since being set?
     *
     * @return true if modified.
     */
    public boolean isModified() {
        return !ObjectUtils.equals(activeLocation, getActiveLocation())
                || !ObjectUtils.equals(configurationLocations, getConfigurationLocations());
    }

    /**
     * Listener to update UI based on radio button selections.
     */
    private class RadioListener implements ActionListener {

        public void actionPerformed(final ActionEvent e) {
            final boolean showModuleConfig = useModuleConfigurationRadio.isSelected();

            configurationFilesLabel.setEnabled(showModuleConfig);
            configurationFilesCombo.setEnabled(showModuleConfig);
        }
    }

}
