package org.infernus.idea.checkstyle.ui;

import com.intellij.util.ui.JBUI;
import org.infernus.idea.checkstyle.CheckStyleBundle;

import javax.swing.*;
import java.awt.*;

public class CompletePanel extends JPanel {

    public CompletePanel() {
        super(new GridBagLayout());

        initialise();
    }

    private void initialise() {
        final JLabel infoLabel = new JLabel(CheckStyleBundle.message("config.file.complete.text"));

        setBorder(JBUI.Borders.empty(4));

        add(infoLabel, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.NORTHWEST,
                GridBagConstraints.NONE, JBUI.insets(8), 0, 0));
    }
}
