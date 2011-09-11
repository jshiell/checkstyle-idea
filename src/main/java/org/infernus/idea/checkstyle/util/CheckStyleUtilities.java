package org.infernus.idea.checkstyle.util;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;

/**
 * General utilities to make life easier with regards to CheckStyle.
 */
public final class CheckStyleUtilities {

    /**
     * This is a utility class and cannot be instantiated.
     */
    private CheckStyleUtilities() {

    }

    /**
     * Is this file type supported by CheckStyle?
     *
     * @param fileType the file type to test.
     * @return true if this file is supported by CheckStyle.
     */
    public static boolean isValidFileType(final FileType fileType) {
        return fileType != null && StdFileTypes.JAVA.equals(fileType);
    }
}
