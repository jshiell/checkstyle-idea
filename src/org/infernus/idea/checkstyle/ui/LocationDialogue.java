package org.infernus.idea.checkstyle.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.WindowManager;
import org.infernus.idea.checkstyle.CheckStyleConstants;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.util.IDEAUtilities;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Allows selection of the location of the CheckStyle file.
 */
public class LocationDialogue extends JDialog {

    private final JButton browseButton = new JButton(new BrowseAction());
    private final JTextField fileLocationField = new JTextField(20);
    private final JTextField urlLocationField = new JTextField(20);
    private final JRadioButton fileLocationRadio = new JRadioButton();
    private final JRadioButton urlLocationRadio = new JRadioButton();
    private final JTextField descriptionField = new JTextField();

    private final Project project;

    private boolean committed = true;

    /**
     * Create a dialogue.
     *
     * @param project the current project.
     */
    public LocationDialogue(final Project project) {
        super(WindowManager.getInstance().getFrame(project));

        if (project == null) {
            throw new IllegalArgumentException("Project may not be null");
        }
        this.project = project;

        initialise();
    }

    public void initialise() {
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(300, 200));
        setModal(true);

        final JPanel contentPanel = buildContentPanel();

        final JButton okayButton = new JButton(new OkayAction());
        final JButton cancelButton = new JButton(new CancelAction());

        final JPanel bottomPanel = new JPanel(new GridBagLayout());
        bottomPanel.setBorder(new EmptyBorder(4, 8, 8, 8));

        bottomPanel.add(Box.createHorizontalGlue(), new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));

        if (IDEAUtilities.isMacOSX()) {
            bottomPanel.add(cancelButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
            bottomPanel.add(okayButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
        } else {
            bottomPanel.add(okayButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
            bottomPanel.add(cancelButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
        }

        add(contentPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(okayButton);

        pack();

        final Toolkit toolkit = Toolkit.getDefaultToolkit();
        setLocation((toolkit.getScreenSize().width - getSize().width) / 2,
                (toolkit.getScreenSize().height - getSize().height) / 2);
    }

    private JPanel buildContentPanel() {
        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);

        fileLocationRadio.setText(resources.getString("config.file.file.text"));
        fileLocationRadio.addActionListener(new RadioButtonActionListener());
        urlLocationRadio.setText(resources.getString("config.file.url.text"));
        urlLocationRadio.addActionListener(new RadioButtonActionListener());

        final ButtonGroup locationGroup = new ButtonGroup();
        locationGroup.add(fileLocationRadio);
        locationGroup.add(urlLocationRadio);

        fileLocationRadio.setSelected(true);
        enabledFileLocation();

        final JLabel descriptionLabel = new JLabel(resources.getString("config.file.description.text"));
        descriptionField.setToolTipText(resources.getString("config.file.description.tooltip"));

        final JLabel fileLocationLabel = new JLabel(resources.getString("config.file.file.label"));

        final JLabel urlLocationlabel = new JLabel(resources.getString("config.file.url.label"));

        final JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(new EmptyBorder(8, 8, 4, 8));

        contentPanel.add(descriptionLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
        contentPanel.add(descriptionField, new GridBagConstraints(1, 0, 2, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));

        contentPanel.add(fileLocationRadio, new GridBagConstraints(0, 1, 3, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));

        contentPanel.add(fileLocationLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
        contentPanel.add(fileLocationField, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
        contentPanel.add(browseButton, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));

        contentPanel.add(urlLocationRadio, new GridBagConstraints(0, 3, 3, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(8, 4, 4, 4), 0, 0));

        contentPanel.add(urlLocationlabel, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
        contentPanel.add(urlLocationField, new GridBagConstraints(1, 4, 2, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));

        contentPanel.add(Box.createVerticalGlue(), new GridBagConstraints(0, 5, 3, 1, 0.0, 1.0,
                GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(4, 4, 4, 4), 0, 0));

        return contentPanel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVisible(final boolean visible) {
        if (visible) {
            this.committed = false;
        }
        super.setVisible(visible);
    }

    /**
     * Get the configuration location entered in the dialogue, or null if no valid location was entered.
     *
     * @return the location or null if no valid location entered.
     */
    public ConfigurationLocation getConfigurationLocation() {
        if (fileLocationField.isEnabled()) {
            if (StringUtils.isNotBlank(fileLocationField.getText())) {
                return ConfigurationLocationFactory.create(project, ConfigurationType.FILE,
                        fileLocationField.getText(), descriptionField.getText());
            }

        } else if (urlLocationField.isEnabled()) {
            if (StringUtils.isNotBlank(urlLocationField.getText())) {
                return ConfigurationLocationFactory.create(project, ConfigurationType.HTTP_URL,
                        urlLocationField.getText(), descriptionField.getText());
            }
        }

        return null;
    }

    /**
     * Set the configuration location.
     *
     * @param configurationLocation the location.
     */
    public void setConfigurationLocation(final ConfigurationLocation configurationLocation) {
        if (configurationLocation == null) {
            fileLocationRadio.setEnabled(true);
            fileLocationField.setText(null);

        } else if (configurationLocation.getType() == ConfigurationType.FILE) {
            fileLocationRadio.setEnabled(true);
            fileLocationField.setText(configurationLocation.getLocation());


        } else if (configurationLocation.getType() == ConfigurationType.HTTP_URL) {
            urlLocationRadio.setEnabled(true);
            urlLocationField.setText(configurationLocation.getLocation());


        } else {
            throw new IllegalArgumentException("Unsupported configuration type: " + configurationLocation.getType());
        }
    }

    /**
     * Was okay clicked?
     *
     * @return true if okay clicked, false if cancelled.
     */
    public boolean isCommitted() {
        return committed;
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

    /**
     * Respond to an okay action.
     */
    private class OkayAction extends AbstractAction {
        private static final long serialVersionUID = 3800521701284308642L;

        /**
         * Create a new action.
         */
        public OkayAction() {
            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);

            putValue(Action.NAME, resources.getString(
                    "config.file.okay.text"));
            putValue(Action.SHORT_DESCRIPTION,
                    resources.getString("config.file.okay.tooltip"));
            putValue(Action.LONG_DESCRIPTION,
                    resources.getString("config.file.okay.tooltip"));
        }

        public void actionPerformed(final ActionEvent event) {
            final ConfigurationLocation location = getConfigurationLocation();

            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);

            if (location == null) {
                Messages.showErrorDialog(project, resources.getString("config.file.no-file"),
                        resources.getString("config.file.error.title"));
                return;
            }

            try {
                location.resolve();

            } catch (IOException e) {
                final String message = resources.getString("config.file.resolve-failed");
                final String formattedMessage = new MessageFormat(message).format(new Object[]{e.getMessage()});
                Messages.showErrorDialog(project, formattedMessage, resources.getString("config.file.error.title"));

                return;
            }

            committed = true;
            setVisible(false);
        }
    }

    /**
     * Respond to a cancel action.
     */
    private class CancelAction extends AbstractAction {
        private static final long serialVersionUID = -994620715558602656L;

        /**
         * Create a new action.
         */
        public CancelAction() {
            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);

            putValue(Action.NAME, resources.getString(
                    "config.file.cancel.text"));
            putValue(Action.SHORT_DESCRIPTION,
                    resources.getString("config.file.cancel.tooltip"));
            putValue(Action.LONG_DESCRIPTION,
                    resources.getString("config.file.cancel.tooltip"));
        }

        public void actionPerformed(final ActionEvent e) {
            committed = false;

            setVisible(false);
        }
    }

    /**
     * Process a click on the browse button.
     */
    protected final class BrowseAction extends AbstractAction {
        private static final long serialVersionUID = -992858528081327052L;

        /**
         * Create a new browse action.
         */
        public BrowseAction() {
            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);

            putValue(Action.NAME, resources.getString(
                    "config.file.browse.text"));
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
            fileChooser.setFileFilter(new ExtensionFileFilter("xml"));

            final String configFilePath = fileLocationField.getText();
            if (configFilePath != null) {
                fileChooser.setSelectedFile(new File(configFilePath));
            }

            final int result = fileChooser.showOpenDialog(LocationDialogue.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                final File newConfigFile = fileChooser.getSelectedFile();
                fileLocationField.setText(newConfigFile.getAbsolutePath());
            }
        }
    }

    /**
     * Handles radio button selections.
     */
    protected final class RadioButtonActionListener implements ActionListener {
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
