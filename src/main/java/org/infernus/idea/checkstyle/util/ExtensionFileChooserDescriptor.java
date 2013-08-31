package org.infernus.idea.checkstyle.util;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.Serializable;

/**
 * Custom FileChooser Descriptor that allows the specification of a file extension.
 */
public class ExtensionFileChooserDescriptor extends FileChooserDescriptor {
    private static final String JAR = "jar";
    private final String fileExtension;

    /**
     * Construct a file chooser descriptor for the given file extension.
     *
     * @param title       the dialog title.
     * @param description the dialog description.
     * @param fileExt     the file extension.
     */
    public ExtensionFileChooserDescriptor(final String title, final String description, final String fileExt) {
        // select a single file, not jars or jar contents
        super(true, false, fileExt.equalsIgnoreCase(JAR), fileExt.equalsIgnoreCase(JAR), false, false);
        setTitle(title);
        setDescription(description);
        fileExtension = fileExt;
    }

    @Override
    public boolean isFileSelectable(VirtualFile file) {
        final String extension = file.getExtension();
        return Comparing.strEqual(extension, fileExtension);
    }

    @Override
    public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
        if (file.isDirectory()) {
            return true;
        }
        final String extension = file.getExtension();
        return Comparing.strEqual(extension, fileExtension);
    }
}
