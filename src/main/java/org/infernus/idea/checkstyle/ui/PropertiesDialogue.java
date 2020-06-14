package org.infernus.idea.checkstyle.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.ui.JBUI;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Allows setting of file properties.
 */
public class PropertiesDialogue extends JDialog {
    private final PropertiesPanel propertiesPanel;

    private boolean committed = true;

    public PropertiesDialogue(@Nullable final Dialog parent,
                              @NotNull final Project project,
                              @NotNull final CheckstyleProjectService checkstyleProjectService) {
        super(parent);

        this.propertiesPanel = new PropertiesPanel(project, checkstyleProjectService);

        initialise();
    }

    public void initialise() {
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(300, 200));
        setModal(true);

        final JButton okayButton = new JButton(new OkayAction());
        final JButton cancelButton = new JButton(new CancelAction());

        final JPanel bottomPanel = new JPanel(new GridBagLayout());
        bottomPanel.setBorder(JBUI.Borders.empty(4, 8, 8, 8));

        bottomPanel.add(Box.createHorizontalGlue(), new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, JBUI.insets(4), 0, 0));

        if (SystemInfo.isMac) {
            bottomPanel.add(cancelButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.NONE, JBUI.insets(4), 0, 0));
            bottomPanel.add(okayButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.NONE, JBUI.insets(4), 0, 0));
        } else {
            bottomPanel.add(okayButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.NONE, JBUI.insets(4), 0, 0));
            bottomPanel.add(cancelButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.NONE, JBUI.insets(4), 0, 0));
        }

        add(propertiesPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(okayButton);

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
        return propertiesPanel.getConfigurationLocation();
    }

    /**
     * Set the configuration location.
     *
     * @param configurationLocation the location.
     */
    public void setConfigurationLocation(final ConfigurationLocation configurationLocation) {
        propertiesPanel.setConfigurationLocation(configurationLocation);
    }

    /**
     * Was okay clicked?
     *
     * @return true if okay clicked, false if cancelled.
     */
    public boolean isCommitted() {
        return committed;
    }

    /**
     * Respond to an okay action.
     */
    private class OkayAction extends AbstractAction {
        private static final long serialVersionUID = 3800521701284308642L;

        /**
         * Create a new action.
         */
        OkayAction() {
            putValue(Action.NAME, CheckStyleBundle.message(
                    "config.file.okay.text"));
            putValue(Action.SHORT_DESCRIPTION,
                    CheckStyleBundle.message("config.file.okay.tooltip"));
            putValue(Action.LONG_DESCRIPTION,
                    CheckStyleBundle.message("config.file.okay.tooltip"));
        }

        @Override
        public void actionPerformed(final ActionEvent event) {
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
