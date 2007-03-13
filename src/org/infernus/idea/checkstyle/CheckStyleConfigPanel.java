package org.infernus.idea.checkstyle;

import com.intellij.util.ObjectUtils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import java.io.File;

/**
 * Provides an input box and browse button for CheckStyle file selection.
 *
 * @author James Shiell
 * @version 1.0
 */
public final class CheckStyleConfigPanel extends JPanel {

    private final JLabel fileLabel = new JLabel();
    private final JTextField fileField = new JTextField();
    private final JButton browseButton = new JButton();
    private final JTextArea descriptionLabel = new JTextArea();

    private String configFile;

    private CheckStylePlugin plugin;

    /**
     * Create a new panel.
     *
     * @param plugin the plugin that owns this panel.
     */
    public CheckStyleConfigPanel(final CheckStylePlugin plugin) {
        super(new GridBagLayout());

        if (plugin == null) {
            throw new IllegalArgumentException("Plugin may not be null.");
        }

        this.plugin = plugin;

        initialise();
    }

    /**
     * Initialise the view.
     */
    protected void initialise() {
        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);

        fileLabel.setText(resources.getString("config.file.label.text"));
        fileLabel.setToolTipText(resources.getString("config.file.label.tooltip"));

        final GridBagConstraints fileLabelConstraints = new GridBagConstraints(
                0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0);
        add(fileLabel, fileLabelConstraints);

        fileField.setToolTipText(resources.getString("config.file.label.tooltip"));
        fileField.setEditable(false);

        final GridBagConstraints fileFieldConstraints = new GridBagConstraints(
                1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0);
        add(fileField, fileFieldConstraints);

        browseButton.setAction(new BrowseAction());

        final GridBagConstraints browseButtonConstraints = new GridBagConstraints(
                2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0);
        add(browseButton, browseButtonConstraints);

        // fake a multi-line label with a text area
        descriptionLabel.setText(resources.getString(
                "config.file.description"));
        descriptionLabel.setEditable(false);
        descriptionLabel.setEnabled(false);
        descriptionLabel.setWrapStyleWord(true);
        descriptionLabel.setLineWrap(true);
        descriptionLabel.setOpaque(false);
        descriptionLabel.setDisabledTextColor(descriptionLabel.getForeground());

        final GridBagConstraints descLabelConstraints = new GridBagConstraints(
                0, 2, 3, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0);
        add(descriptionLabel, descLabelConstraints);
    }

    /**
     * Get the currently selected configuration file.
     * <p/>
     * This method will only return the configuration file
     * if it currently exists.
     *
     * @return the currently selected configuration file.
     */
    public String getConfigFile() {
        final String fileName = fileField.getText();

        this.configFile = fileName;
        if (fileName != null) {
            final File newConfigFile = new File(fileName);
            if (newConfigFile.exists()) {
                final String filePath = newConfigFile.getAbsolutePath();

                final String projectPath = plugin.getProjectPath();
                if (filePath != null
                        && filePath.startsWith(projectPath)) {
                    return CheckStyleConstants.PROJECT_DIR + filePath.substring(
                            projectPath.length());
                }

                return filePath;
            }
        }
        return null;
    }

    /**
     * Set the configuration file.
     *
     * @param configFile the configuration file.
     */
    public void setConfigFile(final File configFile) {
        if (configFile == null) {
            setConfigFile((String) null);
        } else {
            setConfigFile(configFile.getAbsolutePath());
        }
    }

    /**
     * Set the configuration file.
     *
     * @param configFile the configuration file.
     */
    public void setConfigFile(final String configFile) {
        if (configFile == null) {
            fileField.setText("");
        } else {
            fileField.setText(plugin.processConfigFilePath(configFile));
        }

        this.configFile = configFile;
    }

    /**
     * Reset the configuration to the original values.
     */
    public void reset() {
        setConfigFile(configFile);
    }

    /**
     * Have the settings been modified?
     *
     * @return true if the settngs have been modified.
     */
    public boolean isModified() {
        final String configFileField = fileField.getText();
        final int configFileFieldLength = configFileField != null
                ? configFileField.trim().length() : 0;

        final int originalConfigFileLength = configFile != null
                ? configFile.trim().length() : 0;

        if (configFileFieldLength == 0 && originalConfigFileLength == 0) {
            return true;
        }

        return !ObjectUtils.equals(configFile, fileField.getText());
    }

    /**
     * Process a click on the browse button.
     */
    protected class BrowseAction extends AbstractAction {

        /**
         * Create a new browse action.
         */
        public BrowseAction() {
            super();

            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);
                         
            putValue(Action.NAME, resources.getString("config.file.browse.text"));
            putValue(Action.SHORT_DESCRIPTION,
                    resources.getString("config.file.browse.tooltip"));
            putValue(Action.LONG_DESCRIPTION,
                    resources.getString("config.file.browse.tooltip"));
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(final ActionEvent e) {
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new XMLFileFilter());

            final String configFile = getConfigFile();
            if (configFile != null) {
                fileChooser.setSelectedFile(new File(configFile));
            }

            final int result = fileChooser.showOpenDialog(
                    CheckStyleConfigPanel.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                fileField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        }
    }

    /**
     * File filter for XML files.
     */
    protected class XMLFileFilter extends FileFilter {

        /**
         * The file extension for XML files.
         */
        private static final String XML_EXTENSION = "xml";

        /**
         * {@inheritDoc}
         */
        public boolean accept(final File f) {
            if (f.isDirectory()) {
                return true;
            }
            
            final String fileName = f.getName();
            final int extPos = fileName.lastIndexOf(".");

            return extPos != -1 && extPos != (fileName.length() - 1)
                    && XML_EXTENSION.equalsIgnoreCase(fileName.substring(extPos + 1));
        }

        /**
         * {@inheritDoc}
         */
        public String getDescription() {
            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);
            return resources.getString("config.file.xml.description");
        }
    }

}
