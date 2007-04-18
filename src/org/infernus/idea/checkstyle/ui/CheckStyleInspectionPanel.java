package org.infernus.idea.checkstyle.ui;

import org.infernus.idea.checkstyle.CheckStyleConstants;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * Provides a dummy panel for the inspection configuraiton.
 *
 * @author James Shiell
 * @version 1.0
 */
public final class CheckStyleInspectionPanel extends JPanel {

    /**
     * A text label for a description blurb.
     */
    private final JTextArea descriptionLabel = new JTextArea();

    /**
     * Create a new panel.
     */
    public CheckStyleInspectionPanel() {
        super(new GridBagLayout());

        initialise();
    }

    /**
     * Initialise the view.
     */
    protected void initialise() {
        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);

        // fake a multi-line label with a text area
        descriptionLabel.setText(resources.getString(
                "config.inspection.description"));
        descriptionLabel.setEditable(false);
        descriptionLabel.setEnabled(false);
        descriptionLabel.setWrapStyleWord(true);
        descriptionLabel.setLineWrap(true);
        descriptionLabel.setOpaque(false);
        descriptionLabel.setDisabledTextColor(descriptionLabel.getForeground());

        final GridBagConstraints descLabelConstraints = new GridBagConstraints(
                0, 0, 3, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0);
        add(descriptionLabel, descLabelConstraints);
    }

}
