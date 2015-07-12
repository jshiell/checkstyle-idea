package org.infernus.idea.checkstyle.util;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;

public final class FileTypes {

    private FileTypes() {
    }

    public static boolean isJava(final FileType fileType) {
        return fileType != null && StdFileTypes.JAVA.equals(fileType);
    }

}
