package org.infernus.idea.checkstyle.checker;

import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.util.ScannableFile;

import java.io.IOException;

/**
 * Action to read the file to a temporary file.
 */
class CreateScannableFileAction implements Runnable {

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
    private ScannableFile file;

    /**
     * Create a thread to read the given file to a temporary file.
     *
     * @param psiFile the file to read.
     */
    public CreateScannableFileAction(final PsiFile psiFile) {
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
     * Get the scannable file.
     *
     * @return the scannable file.
     */
    public ScannableFile getFile() {
        return file;
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
        try {
            file = new ScannableFile(psiFile);

        } catch (IOException e) {
            failure = e;
        }
    }
}
