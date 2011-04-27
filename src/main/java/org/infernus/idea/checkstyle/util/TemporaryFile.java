package org.infernus.idea.checkstyle.util;

import com.intellij.psi.PsiFile;

import java.io.File;
import java.util.UUID;

/**
 * A temporary file for scanning modified files.
 */
public class TemporaryFile {
    private static final String TEMPFILE_DIR_PREFIX = "csi-";

    private final File realFile;

    public TemporaryFile(final PsiFile psiFile) {
        final File tmpDir = new File(System.getProperty("java.io.tmpdir"),
                TEMPFILE_DIR_PREFIX + UUID.randomUUID().toString());
        tmpDir.mkdirs();
        tmpDir.deleteOnExit();

        realFile = new File(tmpDir, psiFile.getName());
        realFile.deleteOnExit();
    }

    public File getFile() {
        return realFile;
    }

    public void delete() {
        if (realFile.exists()) {
            realFile.delete();

            if (realFile.getParentFile().getName().startsWith(TEMPFILE_DIR_PREFIX)) {
                realFile.getParentFile().delete();
            }
        }
    }

    public String getAbsolutePath() {
        return realFile.getAbsolutePath();
    }

    @Override
    public String toString() {
        return realFile.toString();
    }
}
