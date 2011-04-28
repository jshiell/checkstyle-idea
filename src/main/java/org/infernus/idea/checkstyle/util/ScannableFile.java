package org.infernus.idea.checkstyle.util;

import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

/**
 * A representation of a file able to be scanned.
 * <p/>
 * At present all files are copied to a temporary file to allow for unsaved data
 * or ongoing editing; this should ideally be optimised so that unmodified files
 * are not copied.
 */
public class ScannableFile {
    private static final String TEMPFILE_DIR_PREFIX = "csi-";

    private final File realFile;

    public ScannableFile(final PsiFile psiFile) throws IOException {
        realFile = createTemporaryFileFor(psiFile);
    }

    private File createTemporaryFileFor(final PsiFile psiFile) throws IOException {
        final File tmpDir = new File(System.getProperty("java.io.tmpdir"),
                TEMPFILE_DIR_PREFIX + UUID.randomUUID().toString());
        tmpDir.mkdirs();
        tmpDir.deleteOnExit();

        final File temporaryFile = new File(tmpDir, psiFile.getName());
        temporaryFile.deleteOnExit();

        writeContentsToFile(psiFile, temporaryFile);

        return temporaryFile;
    }

    private void writeContentsToFile(final PsiFile psiFile,
                                     final File outFile)
            throws IOException {
        final CodeStyleSettings codeStyleSettings
                = CodeStyleSettingsManager.getSettings(psiFile.getProject());

        final BufferedWriter tempFileOut = new BufferedWriter(
                new FileWriter(outFile));
        for (final char character : psiFile.getText().toCharArray()) {
            if (character == '\n') { // IDEA uses \n internally
                tempFileOut.write(codeStyleSettings.getLineSeparator());
            } else {
                tempFileOut.write(character);
            }
        }
        tempFileOut.flush();
        tempFileOut.close();
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
