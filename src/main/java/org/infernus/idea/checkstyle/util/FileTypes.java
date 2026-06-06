package org.infernus.idea.checkstyle.util;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;

public final class FileTypes {

    private FileTypes() {
    }

    public static boolean isJava(final FileType fileType) {
        return JavaFileType.INSTANCE.equals(fileType);
    }

    public static boolean hasJavaExtension(final VirtualFile virtualFile) {
        return "java".equalsIgnoreCase(virtualFile.getExtension());
    }

}
