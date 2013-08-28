package org.infernus.idea.checkstyle.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.WindowManager;
import org.infernus.idea.checkstyle.CheckStyleConstants;
import org.infernus.idea.checkstyle.checker.CheckerFactory;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Allows selection of the location of the CheckStyle file.
 */
public class LocationDialogue extends JDialog {
    private enum CurrentStep {
        SELECT(false, true, false),
        PROPERTIES(true, true, false),
        ERROR(true, false, false),
        COMPLETE(true, false, true);

        private boolean allowPrevious;
        private boolean allowNext;
        private boolean allowCommit;

        private CurrentStep(final boolean allowPrevious, final boolean allowNext, final boolean allowCommit) {
            this.allowPrevious = allowPrevious;
            this.allowNext = allowNext;
            this.allowCommit = allowCommit;
        }

        private boolean isAllowPrevious() {
            return allowPrevious;
        }

        private boolean isAllowNext() {
            return allowNext;
        }

        private boolean isAllowCommit() {
            return allowCommit;
        }
    }

    private final LocationPanel locationPanel;
    private final PropertiesPanel propertiesPanel;
    private final ErrorPanel errorPanel;
    private final CompletePanel completePanel;

    private final Project project;
    private final List<String> thirdPartyClasspath;

    private JButton commitButton;
    private JButton previousButton;
    private CurrentStep currentStep = CurrentStep.SELECT;
    private boolean committed = true;
    private ConfigurationLocation configurationLocation;

    /**
     * Create a dialogue.
     *
     * @param project the current project.
     * @param thirdPartyClasspath the third-party classpath.
     */
    public LocationDialogue(final Project project, final List<String> thirdPartyClasspath) {
        super(WindowManager.getInstance().getFrame(project));

        if (project == null) {
            throw new IllegalArgumentException("Project may not be null");
        }

        this.project = project;
        this.thirdPartyClasspath = thirdPartyClasspath;
        this.locationPanel = new LocationPanel(project);
        this.propertiesPanel = new PropertiesPanel(project);
        this.errorPanel = new ErrorPanel();
        this.completePanel = new CompletePanel();

        initialise();
    }

    public void initialise() {
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(500, 400));
        setModal(true);

        commitButton = new JButton(new NextAction());
        previousButton = new JButton(new PreviousAction());
        final JButton cancelButton = new JButton(new CancelAction());

        final JPanel bottomPanel = new JPanel(new GridBagLayout());
        bottomPanel.setBorder(new EmptyBorder(4, 8, 8, 8));

        if (SystemInfo.isMac) {
            bottomPanel.add(cancelButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
            bottomPanel.add(Box.createHorizontalGlue(), new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
        } else {
            bottomPanel.add(Box.createHorizontalGlue(), new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
            bottomPanel.add(cancelButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
        }
        bottomPanel.add(previousButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
        bottomPanel.add(commitButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));

        add(panelForCurrentStep(), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(commitButton);
        moveToStep(CurrentStep.SELECT);

        pack();

        final Toolkit toolkit = Toolkit.getDefaultToolkit();
        setLocation((toolkit.getScreenSize().width - getSize().width) / 2,
                (toolkit.getScreenSize().height - getSize().height) / 2);
    }

    private JPanel panelForCurrentStep() {
        switch (currentStep) {
            case SELECT:
                return locationPanel;
            case PROPERTIES:
                return propertiesPanel;
            case ERROR:
                return errorPanel;
            case COMPLETE:
                return completePanel;
            default:
                throw new IllegalStateException("Unknown step: " + currentStep);
        }
    }

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
        return configurationLocation;
    }

    private void moveToStep(final CurrentStep newStep) {
        final ResourceBundle resources = ResourceBundle.getBundle(CheckStyleConstants.RESOURCE_BUNDLE);

        remove(panelForCurrentStep());
        currentStep = newStep;

        if (currentStep.isAllowCommit()) {
            commitButton.setText(resources.getString("config.file.okay.text"));
            commitButton.setToolTipText(resources.getString("config.file.okay.text"));
        } else {
            commitButton.setText(resources.getString("config.file.next.text"));
            commitButton.setToolTipText(resources.getString("config.file.next.text"));
        }

        previousButton.setEnabled(currentStep.isAllowPrevious());
        commitButton.setEnabled(currentStep.isAllowNext() || currentStep.isAllowCommit());

        getContentPane().add(panelForCurrentStep(), BorderLayout.CENTER);
        getContentPane().validate();
        getContentPane().repaint();
    }

    /**
     * Was okay clicked?
     *
     * @return true if okay clicked, false if cancelled.
     */
    public boolean isCommitted() {
        return committed;
    }

    private void testLoadOfFile(final ConfigurationLocation location) {
        configurationLocation = location;

        try {
            new CheckerFactory().getChecker(location, thirdPartyClasspath);
        } catch (Exception e) {
            errorPanel.setError(e);
            moveToStep(CurrentStep.ERROR);
            return;
        }

        moveToStep(CurrentStep.COMPLETE);
    }

    private class NextAction extends AbstractAction {
        private static final long serialVersionUID = 3800521701284308642L;

        public void actionPerformed(final ActionEvent event) {
            final ResourceBundle resources = ResourceBundle.getBundle(CheckStyleConstants.RESOURCE_BUNDLE);

            commitButton.setEnabled(false);

            final ConfigurationLocation location;
            switch (currentStep) {
                case SELECT:
                    location = locationPanel.getConfigurationLocation();
                    if (location == null) {
                        showError(resources, resources.getString("config.file.no-file"));
                        return;
                    }

                    if (location.getDescription() == null || location.getDescription().isEmpty()) {
                        showError(resources, resources.getString("config.file.no-description"));
                        return;
                    }

                    Map<String, String> properties;
                    try {
                        location.resolve();
                        properties = location.getProperties();

                    } catch (IOException e) {
                        final String message = resources.getString("config.file.resolve-failed");
                        final String formattedMessage = new MessageFormat(message).format(new Object[]{e.getMessage()});
                        showError(resources, formattedMessage);
                        return;
                    }

                    if (properties == null || properties.isEmpty()) {
                        testLoadOfFile(location);
                    } else {
                        propertiesPanel.setConfigurationLocation(location);
                        moveToStep(CurrentStep.PROPERTIES);
                    }
                    return;

                case PROPERTIES:
                    location = propertiesPanel.getConfigurationLocation();
                    testLoadOfFile(location);
                    return;

                case COMPLETE:
                    committed = true;
                    setVisible(false);
                    return;

                default:
                    throw new IllegalStateException("Unexpected next call for step " + currentStep);
            }
        }
    }

    private void showError(final ResourceBundle resources, final String formattedMessage) {
        Messages.showErrorDialog(this, formattedMessage, resources.getString("config.file.error.title"));
        commitButton.setEnabled(true);
    }

    private class PreviousAction extends AbstractAction {
        private static final long serialVersionUID = 3800521701284308642L;

        public PreviousAction() {
            final ResourceBundle resources = ResourceBundle.getBundle(CheckStyleConstants.RESOURCE_BUNDLE);

            putValue(Action.NAME, resources.getString("config.file.previous.text"));
            putValue(Action.SHORT_DESCRIPTION, resources.getString("config.file.previous.tooltip"));
            putValue(Action.LONG_DESCRIPTION, resources.getString("config.file.previous.tooltip"));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            previousButton.setEnabled(false);

            switch (currentStep) {
                case PROPERTIES:
                    moveToStep(CurrentStep.SELECT);
                    return;

                case COMPLETE:
                case ERROR:
                    try {
                        final Map<String, String> properties = configurationLocation.getProperties();
                        if (properties == null || properties.isEmpty()) {
                            moveToStep(CurrentStep.SELECT);
                        } else {
                            moveToStep(CurrentStep.PROPERTIES);
                        }
                    } catch (IOException e1) {
                        moveToStep(CurrentStep.SELECT);
                    }
                    return;

                default:
                    throw new IllegalStateException("Unexpected previous call for step " + currentStep);
            }
        }
    }

    /**
     * Respond to a cancel action.
     */
    private class CancelAction extends AbstractAction {
        private static final long serialVersionUID = -994620715558602656L;

        public CancelAction() {
            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);

            putValue(Action.NAME, resources.getString("config.file.cancel.text"));
            putValue(Action.SHORT_DESCRIPTION, resources.getString("config.file.cancel.tooltip"));
            putValue(Action.LONG_DESCRIPTION, resources.getString("config.file.cancel.tooltip"));
        }

        public void actionPerformed(final ActionEvent e) {
            committed = false;

            setVisible(false);
        }
    }
}
