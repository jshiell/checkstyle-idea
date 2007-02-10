package org.infernus.idea.checkstyle.util;

import javax.swing.ImageIcon;
import java.net.URL;

/**
 * General utilities to make life easier with regards to IDEA.
 *
 * @author James Shiell
 * @version 1.0
 */
public final class IDEAUtilities {

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


}
