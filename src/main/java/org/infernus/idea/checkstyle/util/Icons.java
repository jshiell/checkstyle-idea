package org.infernus.idea.checkstyle.util;

import javax.swing.*;
import java.net.URL;

public final class Icons {

    private Icons() {
    }

    public static ImageIcon icon(final String iconPath) {
        final URL url = Icons.class.getResource(iconPath);
        if (url != null) {
            return new ImageIcon(url);
        }

        return null;
    }

}
