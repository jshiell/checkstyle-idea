package org.infernus.idea.checkstyle.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.LightColors;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStyleConstants;

import javax.swing.*;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * General utilities to make life easier with regards to IDEA.
 *
 * @author James Shiell
 * @version 1.0
 */
public final class IDEAUtilities {

    /**
     * Logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(
            IDEAUtilities.class);

    /**
     * This is a utility class and cannot be instantiated.
     */
    private IDEAUtilities() {

    }

    /**
     * Are we running on Mac OS X?
     *
     * @return true if this is a Mac OS.
     */
    public static boolean isMacOSX() {
        return System.getProperty("os.name").toLowerCase().contains("mac os x");
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
     * @param project the current project.
     * @param warning the text of the warning.
     */
    public static void showWarning(final Project project,
                                   final String warning) {
        final Runnable showMessage = new Runnable() {
            public void run() {
                final MessageFormat notificationFormat = new MessageFormat(
                        getResource("plugin.notification.format", "<b>CheckStyle:</b> {0}"));
                final String warningText = getResource(warning, warning);

                final JLabel warningComponent = new JLabel("<html>" +
                        notificationFormat.format(new Object[]{warningText}) + "</html>");

                final StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
                if (statusBar != null) {
                    statusBar.fireNotificationPopup(warningComponent, LightColors.YELLOW);
                }
            }
        };

        SwingUtilities.invokeLater(showMessage);
    }

    /**
     * Get a string resource.
     *
     * @param resourceKey the key of the resource.
     * @param fallback    the fallback value if the resource does not exist.
     * @return the resource value.
     */
    public static String getResource(final String resourceKey,
                                     final String fallback) {
        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);

        String resourceValue = null;
        if (resources == null) {
            LOG.warn("Resource bundle was not found: "
                    + CheckStyleConstants.RESOURCE_BUNDLE);

        } else {
            resourceValue = resources.getString(resourceKey);

            if (resourceValue == null) {
                LOG.warn(resourceKey + " was not defined in resource bundle.");
            }
        }

        if (resourceValue == null) { // fallback
            resourceValue = fallback;
        }

        return resourceValue;
    }

}
