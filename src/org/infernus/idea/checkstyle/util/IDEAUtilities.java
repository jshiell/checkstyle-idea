package org.infernus.idea.checkstyle.util;

import org.infernus.idea.checkstyle.CheckStyleConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.ImageIcon;
import java.net.URL;
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
