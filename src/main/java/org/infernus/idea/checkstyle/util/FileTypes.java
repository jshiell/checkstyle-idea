package org.infernus.idea.checkstyle.util;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.fileTypes.FileType;

public final class FileTypes {

    private FileTypes() {
    }

    public static boolean isJava(final FileType fileType) {
        return JavaFileType.INSTANCE.equals(fileType);
    }

}
