package org.infernus.idea.checkstyle.checker;

import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import org.infernus.idea.checkstyle.util.TemporaryFile;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Thread to read the file to a temporary file.
 */
class CreateTempFileThread implements Runnable {

    /**
     * Any failure that occurred on the thread.
     */
    private IOException failure;

    /**
     * The file we are creating a temporary file from.
     */
    private PsiFile psiFile;

    /**
     * The created temporary file.
     */
    private TemporaryFile file;

    /**
     * Create a thread to read the given file to a temporary file.
     *
     * @param psiFile the file to read.
     */
    public CreateTempFileThread(final PsiFile psiFile) {
        this.psiFile = psiFile;
    }

    /**
         * Get any failure that occurred in this thread.
         *
         * @return the failure, if any.
         */
        public IOException getFailure() {
            return failure;
        }

        /**
         * Get the temporary file.
         *
         * @return the temporary file.
         */
        public TemporaryFile getFile() {
            return file;
        }

        /**
         * {@inheritDoc}
         */
        public void run() {
            try {
                file = new TemporaryFile(psiFile);

                final CodeStyleSettings codeStyleSettings
                        = CodeStyleSettingsManager.getSettings(psiFile.getProject());

                final BufferedWriter tempFileOut = new BufferedWriter(
                        new FileWriter(file.getFile()));
                for (final char character : psiFile.getText().toCharArray()) {
                    if (character == '\n') { // IDEA uses \n internally
                        tempFileOut.write(codeStyleSettings.getLineSeparator());
                    } else {
                        tempFileOut.write(character);
                    }
                }
                tempFileOut.flush();
                tempFileOut.close();

            } catch (IOException e) {
                failure = e;
            }
        }
}
