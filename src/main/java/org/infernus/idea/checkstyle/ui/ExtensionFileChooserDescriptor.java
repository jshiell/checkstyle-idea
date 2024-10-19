package org.infernus.idea.checkstyle.ui;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNullElse;

/**
 * Custom FileChooser Descriptor that allows the specification of a file extension.
 */
public class ExtensionFileChooserDescriptor extends FileChooserDescriptor {

    private final Set<String> fileExtensions;

    /**
     * Construct a file chooser descriptor for the given file extension.
     *
     * @param title            the dialog title.
     * @param description      the dialog description.
     * @param allowFilesInJars may files within JARs be selected?
     * @param fileExtensions   the file extension(s).
     */
    public ExtensionFileChooserDescriptor(final String title,
                                          final String description,
                                          final boolean allowFilesInJars,
                                          final String... fileExtensions) {
        super(true, false, containsJar(fileExtensions), containsJar(fileExtensions), allowFilesInJars, false);

        setTitle(title);
        setDescription(description);

        // well, this is clumsy...
        withFileFilter(this::fileExtensionMatches);

        this.fileExtensions = lowerCase(fileExtensions);
    }

    private static boolean containsJar(final String[] extensions) {
        if (extensions == null) {
            return false;
        }
        for (final String extension : extensions) {
            if ("jar".equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> lowerCase(final String[] strings) {
        return Arrays.stream(requireNonNullElse(strings, new String[0]))
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isFileSelectable(final VirtualFile file) {
        return fileExtensionMatches(file);
    }

    private boolean fileExtensionMatches(final VirtualFile file) {
        final String currentExtension = file.getExtension();
        return currentExtension != null && fileExtensions.contains(currentExtension);
    }
}
