package org.infernus.idea.checkstyle.ui;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.infernus.idea.checkstyle.CheckStyleBundle;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;

import static javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS;
import static javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS;

public class ErrorPanel extends JPanel {
    private final JTextArea errorField = new JTextArea();

    public ErrorPanel() {
        super(new BorderLayout());

        initialise();

        setError(new RuntimeException());
    }

    private void initialise() {
        setBorder(JBUI.Borders.empty(8));

        final JLabel infoLabel = new JLabel(CheckStyleBundle.message("config.file.error.caption"));
        infoLabel.setBorder(JBUI.Borders.emptyBottom(8));
        add(infoLabel, BorderLayout.NORTH);

        errorField.setEditable(false);
        errorField.setTabSize(2);
        errorField.setWrapStyleWord(true);
        errorField.setLineWrap(true);

        final JScrollPane errorScrollPane = new JBScrollPane(errorField, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_ALWAYS);
        add(errorScrollPane, BorderLayout.CENTER);
    }

    public void setError(final Throwable t) {
        final StringWriter errorWriter = new StringWriter();
        causeOf(t).printStackTrace(new PrintWriter(errorWriter));

        errorField.setText(errorWriter.getBuffer().toString());
        errorField.setCaretPosition(0);
        invalidate();
    }

    private Throwable causeOf(final Throwable t) {
        if (t.getCause() != null && t.getCause() != t
                && !t.getClass().getPackage().getName().startsWith("com.puppycrawl")) {
            return causeOf(t.getCause());
        }
        return t;
    }
}
