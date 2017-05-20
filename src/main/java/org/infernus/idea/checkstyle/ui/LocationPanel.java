package org.infernus.idea.checkstyle.ui;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.JBUI;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import static org.infernus.idea.checkstyle.util.Strings.isBlank;

public class LocationPanel extends JPanel {

    private static final Insets COMPONENT_INSETS = JBUI.insets(4);
    private final JButton browseButton = new JButton(new BrowseAction());
    private final JTextField fileLocationField = new JTextField(20);
    private final JTextField urlLocationField = new JTextField(20);
    private final JRadioButton fileLocationRadio = new JRadioButton();
    private final JRadioButton urlLocationRadio = new JRadioButton();
    private final JTextField descriptionField = new JTextField();
    private final JCheckBox relativeFileCheckbox = new JCheckBox();
    private final JCheckBox insecureHttpCheckbox = new JCheckBox();

    private final Project project;

    public LocationPanel(final Project project) {
        super(new GridBagLayout());

        if (project == null) {
            throw new IllegalArgumentException("Project may not be null");
        }
        this.project = project;

        initialise();
    }

    private void initialise() {
        relativeFileCheckbox.setText(CheckStyleBundle.message("config.file.relative-file.text"));
        relativeFileCheckbox.setToolTipText(CheckStyleBundle.message("config.file.relative-file.tooltip"));
        insecureHttpCheckbox.setText(CheckStyleBundle.message("config.file.insecure-http.text"));
        insecureHttpCheckbox.setToolTipText(CheckStyleBundle.message("config.file.insecure-http.tooltip"));

        fileLocationRadio.setText(CheckStyleBundle.message("config.file.file.text"));
        fileLocationRadio.addActionListener(new RadioButtonActionListener());
        urlLocationRadio.setText(CheckStyleBundle.message("config.file.url.text"));
        urlLocationRadio.addActionListener(new RadioButtonActionListener());

        final ButtonGroup locationGroup = new ButtonGroup();
        locationGroup.add(fileLocationRadio);
        locationGroup.add(urlLocationRadio);

        fileLocationRadio.setSelected(true);
        enabledFileLocation();

        final JLabel descriptionLabel = new JLabel(CheckStyleBundle.message("config.file.description.text"));
        descriptionField.setToolTipText(CheckStyleBundle.message("config.file.description.tooltip"));

        final JLabel fileLocationLabel = new JLabel(CheckStyleBundle.message("config.file.file.label"));
        final JLabel urlLocationlabel = new JLabel(CheckStyleBundle.message("config.file.url.label"));

        setBorder(new EmptyBorder(8, 8, 4, 8));

        add(descriptionLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
        add(descriptionField, new GridBagConstraints(1, 0, 2, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, COMPONENT_INSETS, 0, 0));

        add(fileLocationRadio, new GridBagConstraints(0, 1, 3, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));

        add(fileLocationLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
        add(fileLocationField, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, COMPONENT_INSETS, 0, 0));
        add(browseButton, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));

        add(relativeFileCheckbox, new GridBagConstraints(1, 3, 2, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));

        add(urlLocationRadio, new GridBagConstraints(0, 4, 3, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, JBUI.insets(8, 4, 4, 4), 0, 0));

        add(urlLocationlabel, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
        add(urlLocationField, new GridBagConstraints(1, 5, 2, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, COMPONENT_INSETS, 0, 0));

        add(insecureHttpCheckbox, new GridBagConstraints(1, 6, 2, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));

        add(Box.createVerticalGlue(), new GridBagConstraints(0, 7, 3, 1, 0.0, 1.0,
                GridBagConstraints.WEST, GridBagConstraints.VERTICAL, COMPONENT_INSETS, 0, 0));
    }

    private void enabledFileLocation() {
        fileLocationField.setEnabled(true);
        browseButton.setEnabled(true);

        urlLocationField.setEnabled(false);
    }

    private void enabledURLLocation() {
        fileLocationField.setEnabled(false);
        browseButton.setEnabled(false);

        urlLocationField.setEnabled(true);
    }

    private ConfigurationType typeOfFile() {
        if (relativeFileCheckbox.isSelected()) {
            return ConfigurationType.PROJECT_RELATIVE;
        }
        return ConfigurationType.LOCAL_FILE;
    }

    private ConfigurationType typeOfUrl() {
        if (insecureHttpCheckbox.isSelected()) {
            return ConfigurationType.INSECURE_HTTP_URL;
        }
        return ConfigurationType.HTTP_URL;
    }

    /**
     * Get the configuration location entered in the dialogue, or null if no valid location was entered.
     *
     * @return the location or null if no valid location entered.
     */
    public ConfigurationLocation getConfigurationLocation() {
        if (fileLocationField.isEnabled()) {
            if (isNotBlank(fileLocation())) {
                return configurationLocationFactory().create(project, typeOfFile(),
                        fileLocation(), descriptionField.getText());
            }

        } else if (urlLocationField.isEnabled()) {
            if (isNotBlank(urlLocation())) {
                return configurationLocationFactory().create(project, typeOfUrl(),
                        urlLocation(), descriptionField.getText());
            }
        }

        return null;
    }

    private String urlLocation() {
        return trim(urlLocationField.getText());
    }

    private String fileLocation() {
        return trim(fileLocationField.getText());
    }

    private String trim(final String text) {
        if (text != null) {
            return text.trim();
        }
        return null;
    }

    private ConfigurationLocationFactory configurationLocationFactory() {
        return ServiceManager.getService(project, ConfigurationLocationFactory.class);
    }

    /*
     * This is a port from commons-lang 2.4, in order to get around the absence of commons-lang in
     * some packages of IDEA 7.x.
     */
    private boolean isNotBlank(final String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return false;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }


    /**
     * Set the configuration location.
     *
     * @param configurationLocation the location.
     */
    public void setConfigurationLocation(final ConfigurationLocation configurationLocation) {
        relativeFileCheckbox.setSelected(false);
        insecureHttpCheckbox.setSelected(false);

        if (configurationLocation == null) {
            fileLocationRadio.setEnabled(true);
            fileLocationField.setText(null);

        } else if (configurationLocation.getType() == ConfigurationType.LOCAL_FILE
                || configurationLocation.getType() == ConfigurationType.PROJECT_RELATIVE) {
            fileLocationRadio.setEnabled(true);
            fileLocationField.setText(configurationLocation.getLocation());
            relativeFileCheckbox.setSelected(configurationLocation.getType() == ConfigurationType.PROJECT_RELATIVE);

        } else if (configurationLocation.getType() == ConfigurationType.HTTP_URL
                || configurationLocation.getType() == ConfigurationType.INSECURE_HTTP_URL) {
            urlLocationRadio.setEnabled(true);
            urlLocationField.setText(configurationLocation.getLocation());
            insecureHttpCheckbox.setSelected(configurationLocation.getType() == ConfigurationType.INSECURE_HTTP_URL);

        } else {
            throw new IllegalArgumentException("Unsupported configuration type: " + configurationLocation.getType());
        }
    }

    private final class BrowseAction extends AbstractAction {
        private static final long serialVersionUID = -992858528081327052L;

        BrowseAction() {
            putValue(Action.NAME, CheckStyleBundle.message(
                    "config.file.browse.text"));
            putValue(Action.SHORT_DESCRIPTION,
                    CheckStyleBundle.message("config.file.browse.tooltip"));
            putValue(Action.LONG_DESCRIPTION,
                    CheckStyleBundle.message("config.file.browse.tooltip"));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            final VirtualFile toSelect;
            final String configFilePath = fileLocation();
            if (!isBlank(configFilePath)) {
                toSelect = LocalFileSystem.getInstance().findFileByPath(configFilePath);
            } else {
                toSelect = project.getBaseDir();
            }

            final FileChooserDescriptor descriptor = new ExtensionFileChooserDescriptor(
                    (String) getValue(Action.NAME),
                    (String) getValue(Action.SHORT_DESCRIPTION),
                    true, "xml", "checkstyle");
            final VirtualFile chosen = FileChooser.chooseFile(descriptor, project, toSelect);
            if (chosen != null) {
                final File newConfigFile = VfsUtilCore.virtualToIoFile(chosen);
                fileLocationField.setText(newConfigFile.getAbsolutePath());
            }
        }
    }

    /**
     * Handles radio button selections.
     */
    private final class RadioButtonActionListener implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent e) {
            if (urlLocationRadio.isSelected()) {
                enabledURLLocation();

            } else if (fileLocationRadio.isSelected()) {
                enabledFileLocation();

            } else {
                throw new IllegalStateException("Unknown radio button state");
            }
        }
    }
}
