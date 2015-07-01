package org.infernus.idea.checkstyle.ui;

import org.infernus.idea.checkstyle.CheckStyleBundle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class CompletePanel extends JPanel {

    public CompletePanel() {
        super(new GridBagLayout());

        initialise();
    }

    private void initialise() {
        final JLabel infoLabel = new JLabel(CheckStyleBundle.message("config.file.complete.text"));

        setBorder(new EmptyBorder(4, 4, 4, 4));

        add(infoLabel, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.NORTHWEST,
                GridBagConstraints.NONE, new Insets(8, 8, 8, 8), 0, 0));
    }
}
