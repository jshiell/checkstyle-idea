package org.infernus.idea.checkstyle.util;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

/**
 * A representation of a file able to be scanned.
 */
public class ScannableFile {
    private static final String TEMPFILE_DIR_PREFIX = "csi-";

    private final File realFile;
    private final boolean temporaryFile;

    public ScannableFile(@NotNull final PsiFile psiFile) throws IOException {
        if (!existsOnFilesystem(psiFile) || fileOpenOrUnsaved(psiFile)) {
            realFile = createTemporaryFileFor(psiFile);
            temporaryFile = true;

        } else {
            realFile = new File(pathOf(psiFile));
            temporaryFile = false;
        }
    }

    private String pathOf(@NotNull final PsiFile psiFile) {
        if (psiFile.getVirtualFile() != null) {
            return psiFile.getVirtualFile().getPath();
        }
        throw new IllegalStateException("PSIFile does not have associated virtual file: " + psiFile);
    }

    private File createTemporaryFileFor(@NotNull final PsiFile psiFile) throws IOException {
        final File temporaryFile = new File(baseDirectoryFor(psiFile), psiFile.getName());
        temporaryFile.deleteOnExit();

        writeContentsToFile(psiFile, temporaryFile);

        return temporaryFile;
    }

    private File baseDirectoryFor(@NotNull final PsiFile psiFile) {
        final File baseTmpDir = new File(System.getProperty("java.io.tmpdir"),
                TEMPFILE_DIR_PREFIX + UUID.randomUUID().toString());
        baseTmpDir.deleteOnExit();

        final File tempDir;
        if (psiFile instanceof PsiJavaFile) {
            final String packageName = ((PsiJavaFile) psiFile).getPackageName();
            final String packagePath = packageName.replaceAll("\\.", File.separator);

            tempDir = new File(baseTmpDir.getAbsolutePath() + File.separator + packagePath);

        } else {
            tempDir = baseTmpDir;
        }

        tempDir.mkdirs();

        return tempDir;
    }

    private boolean existsOnFilesystem(@NotNull final PsiFile psiFile) {
        final VirtualFile virtualFile = psiFile.getVirtualFile();
        return virtualFile != null
                && LocalFileSystem.getInstance().exists(psiFile.getVirtualFile());
    }

    @SuppressWarnings({"SimplifiableIfStatement"})
    private boolean fileOpenOrUnsaved(@NotNull final PsiFile psiFile) {
        final VirtualFile virtualFile = psiFile.getVirtualFile();
        if (virtualFile != null) {
            return FileEditorManager.getInstance(psiFile.getProject()).isFileOpen(virtualFile)
                    || FileDocumentManager.getInstance().isFileModifiedAndDocumentUnsaved(virtualFile);
        }
        return false;
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
        if (temporaryFile) {
            if (realFile.exists() && realFile.getParentFile().getName().startsWith(TEMPFILE_DIR_PREFIX)) {
                realFile.delete();
                realFile.getParentFile().delete();
            }
        }
    }

    public String getAbsolutePath() {
        return realFile.getAbsolutePath();
    }

    @Override
    public String toString() {
        return String.format("[ScannableFile: file=%s; temporary=%s]", realFile.toString(), temporaryFile);
    }
}
