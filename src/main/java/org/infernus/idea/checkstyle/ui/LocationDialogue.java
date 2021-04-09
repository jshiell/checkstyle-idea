package org.infernus.idea.checkstyle.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.ui.JBUI;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.checker.CheckerFactory;
import org.infernus.idea.checkstyle.checker.CheckerFactoryCache;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * Allows selection of the location of the CheckStyle file.
 */
public class LocationDialogue extends JDialog {

    private static final Insets COMPONENT_INSETS = JBUI.insets(4);
    private static final int WIDTH = 500;
    private static final int HEIGHT = 400;

    private enum Step {
        SELECT(false, true, false),
        PROPERTIES(true, true, false),
        ERROR(true, false, false),
        COMPLETE(true, false, true);

        private final boolean allowPrevious;
        private final boolean allowNext;
        private final boolean allowCommit;

        Step(final boolean allowPrevious, final boolean allowNext, final boolean allowCommit) {
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

    private final Project project;
    private final CheckstyleProjectService checkstyleProjectService;
    private final String checkstyleVersion;
    private final List<String> thirdPartyClasspath;

    private final LocationPanel locationPanel;
    private final PropertiesPanel propertiesPanel;
    private final ErrorPanel errorPanel;
    private final CompletePanel completePanel;

    private JButton commitButton;
    private JButton previousButton;
    private Step currentStep = Step.SELECT;
    private boolean committed = true;
    private ConfigurationLocation configurationLocation;


    public LocationDialogue(@Nullable final Dialog parent,
                            @NotNull final Project project,
                            @Nullable final String checkstyleVersion,
                            @Nullable final List<String> thirdPartyClasspath,
                            @NotNull final CheckstyleProjectService checkstyleProjectService) {
        super(parent);

        this.project = project;
        this.checkstyleProjectService = checkstyleProjectService;
        this.checkstyleVersion = checkstyleVersion;
        this.thirdPartyClasspath = thirdPartyClasspath;

        this.locationPanel = new LocationPanel(project);
        this.propertiesPanel = new PropertiesPanel(project, checkstyleProjectService);
        this.errorPanel = new ErrorPanel();
        this.completePanel = new CompletePanel();

        initialise();
    }

    public void initialise() {
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(WIDTH, HEIGHT));
        setModal(true);

        commitButton = new JButton(new NextAction());
        previousButton = new JButton(new PreviousAction());
        final JButton cancelButton = new JButton(new CancelAction());

        final JPanel bottomPanel = new JPanel(new GridBagLayout());
        bottomPanel.setBorder(JBUI.Borders.empty(4, 8, 8, 8));

        if (SystemInfo.isMac) {
            bottomPanel.add(cancelButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
            bottomPanel.add(Box.createHorizontalGlue(), new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, COMPONENT_INSETS, 0, 0));
        } else {
            bottomPanel.add(Box.createHorizontalGlue(), new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, COMPONENT_INSETS, 0, 0));
            bottomPanel.add(cancelButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
        }
        bottomPanel.add(previousButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
        bottomPanel.add(commitButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));

        add(panelForCurrentStep(), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(commitButton);
        moveToStep(Step.SELECT);

        pack();

        addEscapeListener();

        final Toolkit toolkit = Toolkit.getDefaultToolkit();
        setLocation((toolkit.getScreenSize().width - getSize().width) / 2,
                (toolkit.getScreenSize().height - getSize().height) / 2);
    }

    private void addEscapeListener() {
        getRootPane().registerKeyboardAction((event) ->  setVisible(false),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
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

    private void moveToStep(final Step newStep) {
        remove(panelForCurrentStep());
        currentStep = newStep;

        if (currentStep.isAllowCommit()) {
            commitButton.setText(CheckStyleBundle.message("config.file.okay.text"));
            commitButton.setToolTipText(CheckStyleBundle.message("config.file.okay.text"));
        } else {
            commitButton.setText(CheckStyleBundle.message("config.file.next.text"));
            commitButton.setToolTipText(CheckStyleBundle.message("config.file.next.text"));
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

    private Step attemptLoadOfFile(final ConfigurationLocation location) {
        configurationLocation = location;

        try {
            location.reset();
            checkerFactory().verify(location);
            return Step.COMPLETE;

        } catch (Exception e) {
            errorPanel.setError(e);
            return Step.ERROR;
        }
    }

    @NotNull
    private CheckerFactory checkerFactory() {
        final CheckstyleProjectService editedConfigProjectService = CheckstyleProjectService.forVersion(
                project, checkstyleVersion, thirdPartyClasspath);
        return CheckerFactory.create(project, editedConfigProjectService, new CheckerFactoryCache());
    }

    private Step continueWithoutLoadOfFile(final ConfigurationLocation location) {
        configurationLocation = location;
        return Step.COMPLETE;
    }

    private class NextAction extends AbstractAction {
        private static final long serialVersionUID = 3800521701284308642L;

        @Override
        public void actionPerformed(final ActionEvent event) {
            commitButton.setEnabled(false);

            final ConfigurationLocation location;
            switch (currentStep) {
                case SELECT:
                    location = locationPanel.getConfigurationLocation();
                    if (location == null) {
                        showError(CheckStyleBundle.message("config.file.no-file"));
                        return;
                    }

                    if (location.getDescription() == null || location.getDescription().isEmpty()) {
                        showError(CheckStyleBundle.message("config.file.no-description"));
                        return;
                    }

                    Map<String, String> properties;
                    if (!project.isDefault() || location.canBeResolvedInDefaultProject()) {
                        try {
                            location.resolve(checkstyleProjectService.underlyingClassLoader());
                            properties = location.getProperties();

                        } catch (IOException e) {
                            showError(CheckStyleBundle.message("config.file.resolve-failed", e.getMessage()));
                            return;
                        }

                        if (properties == null || properties.isEmpty()) {
                            moveToStep(attemptLoadOfFile(location));
                        } else {
                            propertiesPanel.setConfigurationLocation(location);
                            moveToStep(Step.PROPERTIES);
                        }
                    } else {
                        moveToStep(continueWithoutLoadOfFile(location));
                        return;
                    }
                    return;

                case PROPERTIES:
                    location = propertiesPanel.getConfigurationLocation();
                    moveToStep(attemptLoadOfFile(location));
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

    private void showError(final String formattedMessage) {
        Messages.showErrorDialog(this, formattedMessage, CheckStyleBundle.message("config.file.error.title"));
        commitButton.setEnabled(true);
    }

    private class PreviousAction extends AbstractAction {
        private static final long serialVersionUID = 3800521701284308642L;

        PreviousAction() {
            putValue(Action.NAME, CheckStyleBundle.message("config.file.previous.text"));
            putValue(Action.SHORT_DESCRIPTION, CheckStyleBundle.message("config.file.previous.tooltip"));
            putValue(Action.LONG_DESCRIPTION, CheckStyleBundle.message("config.file.previous.tooltip"));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            previousButton.setEnabled(false);

            switch (currentStep) {
                case PROPERTIES:
                    moveToStep(Step.SELECT);
                    return;

                case COMPLETE:
                case ERROR:
                    final Map<String, String> properties = configurationLocation.getProperties();
                    if (properties == null || properties.isEmpty()) {
                        moveToStep(Step.SELECT);
                    } else {
                        moveToStep(Step.PROPERTIES);
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

        CancelAction() {
            putValue(Action.NAME, CheckStyleBundle.message("config.file.cancel.text"));
            putValue(Action.SHORT_DESCRIPTION, CheckStyleBundle.message("config.file.cancel.tooltip"));
            putValue(Action.LONG_DESCRIPTION, CheckStyleBundle.message("config.file.cancel.tooltip"));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            committed = false;

            setVisible(false);
        }
    }
}
