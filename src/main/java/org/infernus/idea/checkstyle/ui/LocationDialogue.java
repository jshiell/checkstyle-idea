package org.infernus.idea.checkstyle.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfoRt;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.util.List;
import java.util.Map;


/**
 * Allows selection of the location of the CheckStyle file.
 */
public class LocationDialogue extends DialogWrapper {

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

    private final JPanel centrePanel = new JPanel(new BorderLayout());
    private final LocationPanel locationPanel;
    private final PropertiesPanel propertiesPanel;
    private final ErrorPanel errorPanel = new ErrorPanel();
    private final CompletePanel completePanel = new CompletePanel();

    private final JButton commitButton = new JButton(new NextAction());
    private final JButton previousButton = new JButton(new PreviousAction());

    private Step currentStep = Step.SELECT;

    private ConfigurationLocation configurationLocation;


    public LocationDialogue(@Nullable final Dialog parent,
                            @NotNull final Project project,
                            @Nullable final String checkstyleVersion,
                            @Nullable final List<String> thirdPartyClasspath,
                            @NotNull final CheckstyleProjectService checkstyleProjectService) {
        super(project, parent, false, IdeModalityType.IDE);

        this.project = project;
        this.checkstyleProjectService = checkstyleProjectService;
        this.checkstyleVersion = checkstyleVersion;
        this.thirdPartyClasspath = thirdPartyClasspath;

        this.locationPanel = new LocationPanel(project);
        this.propertiesPanel = new PropertiesPanel(project, checkstyleProjectService);

        initialiseComponents();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return centrePanel;
    }

    private void initialiseComponents() {
        setTitle(CheckStyleBundle.message("config.file.add.title"));
        setSize(WIDTH, HEIGHT);

        centrePanel.add(panelForCurrentStep(), BorderLayout.CENTER);

        getRootPane().setDefaultButton(commitButton);
        moveToStep(Step.SELECT);

        init();
    }

    @Override
    protected JComponent createSouthPanel() {
        final JPanel bottomPanel = new JPanel(new GridBagLayout());
        bottomPanel.setBorder(JBUI.Borders.empty(4, 8, 8, 8));

        final JButton cancelButton = new JButton(getCancelAction());

        if (SystemInfoRt.isMac) {
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

        return bottomPanel;
    }

    private JPanel panelForCurrentStep() {
        return switch (currentStep) {
            case SELECT -> locationPanel;
            case PROPERTIES -> propertiesPanel;
            case ERROR -> errorPanel;
            case COMPLETE -> completePanel;
        };
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
        centrePanel.remove(panelForCurrentStep());
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

        centrePanel.add(panelForCurrentStep(), BorderLayout.CENTER);
        centrePanel.validate();
        centrePanel.repaint();
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

    private final class NextAction extends AbstractAction {
        @Serial
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
                        try (InputStream ignored = location.resolve(checkstyleProjectService.underlyingClassLoader())) {
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
                    close(OK_EXIT_CODE);
                    return;

                default:
                    throw new IllegalStateException("Unexpected next call for step " + currentStep);
            }
        }
    }

    private void showError(final String formattedMessage) {
        Messages.showErrorDialog(getContentPanel(), formattedMessage, CheckStyleBundle.message("config.file.error.title"));
        commitButton.setEnabled(true);
    }

    private class PreviousAction extends AbstractAction {
        @Serial
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
}
