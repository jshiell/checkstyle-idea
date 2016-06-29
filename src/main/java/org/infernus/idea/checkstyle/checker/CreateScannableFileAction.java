package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Action to read the file to a temporary file.
 */
class CreateScannableFileAction implements Runnable {

    /**
     * Any failure that occurred on the thread.
     */
    private IOException failure;

    private final PsiFile psiFile;
    private final Module module;

    /**
     * The created temporary file.
     */
    private ScannableFile file;

    /**
     * Create a thread to read the given file to a temporary file.
     *
     * @param psiFile the file to read.
     * @param module  the module the file belongs to.
     */
    CreateScannableFileAction(@NotNull final PsiFile psiFile,
                              @Nullable final Module module) {
        this.psiFile = psiFile;
        this.module = module;
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

    @Override
    public void run() {
        try {
            file = new ScannableFile(psiFile, module);

        } catch (IOException e) {
            failure = e;
        }
    }
}
