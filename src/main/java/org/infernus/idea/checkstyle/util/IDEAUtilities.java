package org.infernus.idea.checkstyle.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.LightColors;

import javax.swing.*;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import java.awt.*;
import java.net.URL;

/**
 * General utilities to make life easier with regards to IDEA.
 */
public final class IDEAUtilities {

    private static final int PREFERRED_ALERT_WIDTH = 400;

    private IDEAUtilities() {
    }

    /**
     * Get an internal IDEA icon.
     *
     * @param icon the relative path to the icon.
     * @return the matching icon, or null if none.
     */
    public static ImageIcon getIcon(final String icon) {
        final URL url = IDEAUtilities.class.getResource(icon);
        if (url != null) {
            return new ImageIcon(url);
        }

        return null;
    }

    /**
     * Alert the user to a warning.
     *
     * @param project     the current project.
     * @param warningText the text of the warning.
     */
    public static void showWarning(final Project project,
                                   final String warningText) {
        showMessage(project, warningText, LightColors.YELLOW, IDEAUtilities.getIcon("/general/warning.png"));
    }

    /**
     * Alert the user to an error.
     *
     * @param project   the current project.
     * @param errorText the text of the error.
     */
    public static void showError(final Project project,
                                 final String errorText) {
        showMessage(project, errorText, LightColors.RED, IDEAUtilities.getIcon("/general/error.png"));
    }

    /**
     * Alert the user to a warning.
     *
     * @param project     the current project.
     * @param messageText the text of the warning.
     * @param colour      the background colour to use.
     * @param icon        the icon to display. May be null.
     */
    private static void showMessage(final Project project,
                                    final String messageText,
                                    final Color colour,
                                    final Icon icon) {
        final Runnable showMessage = new Runnable() {
            public void run() {
                final JLabel messageLabel = new JLabel(messageText);
                final Dimension messageDimension = getPreferredSize(messageLabel, PREFERRED_ALERT_WIDTH);
                if (messageDimension != null) {
                    messageLabel.setPreferredSize(messageDimension);
                }
                if (icon != null) {
                    messageLabel.setIcon(icon);
                    messageLabel.setIconTextGap(8);
                    messageLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
                }

                final StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
                if (statusBar != null) {
                    statusBar.fireNotificationPopup(messageLabel, colour);
                }
            }

            private Dimension getPreferredSize(final JLabel label,
                                               final int preferredWidth) {
                final View view = (View) label.getClientProperty(BasicHTML.propertyKey);

                if (view != null) {
                    view.setSize(preferredWidth, 0);
                    return new Dimension((int) Math.ceil(view.getPreferredSpan(View.X_AXIS)),
                            (int) Math.ceil(view.getPreferredSpan(View.Y_AXIS)));
                }
                return null;
            }
        };

        SwingUtilities.invokeLater(showMessage);
    }

}
