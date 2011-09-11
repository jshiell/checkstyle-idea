package org.infernus.idea.checkstyle.ui;

import org.infernus.idea.checkstyle.CheckStyleConstants;
import org.jetbrains.annotations.NotNull;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.ResourceBundle;

/**
 * Filters given a file extension.
 */
public class ExtensionFileFilter extends FileFilter {

    private final String extension;

    /**
     * Create a filter for the given extension.
     *
     * @param extension the extension.
     */
    public ExtensionFileFilter(@NotNull final String extension) {
        this.extension = extension;
    }

    public boolean accept(final File f) {
        if (f.isDirectory()) {
            return true;
        }

        final String fileName = f.getName();
        return fileName.toLowerCase().endsWith("." + extension);
    }

    public String getDescription() {
        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);
        return resources.getString("config.file." + extension
                + ".description");
    }
}
