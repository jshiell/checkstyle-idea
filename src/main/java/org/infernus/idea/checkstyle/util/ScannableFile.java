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
    private final File baseTempDir;

    /**
     * Create a new scannable file from a PSI file.
     * <p/>
     * If required this will create a temporary copy of the file.
     *
     * @param psiFile the psiFile to create the file from.
     * @throws IOException if file creation is required and fails.
     */
    public ScannableFile(@NotNull final PsiFile psiFile)
            throws IOException {
        if (!existsOnFilesystem(psiFile) || fileOpenOrUnsaved(psiFile)) {
            baseTempDir = prepareBaseTmpDir();
            realFile = createTemporaryFileFor(psiFile, baseTempDir);

        } else {
            baseTempDir = null;
            realFile = new File(pathOf(psiFile));
        }
    }

    private String pathOf(@NotNull final PsiFile psiFile) {
        if (psiFile.getVirtualFile() != null) {
            return psiFile.getVirtualFile().getPath();
        }
        throw new IllegalStateException("PSIFile does not have associated virtual file: " + psiFile);
    }

    private File createTemporaryFileFor(@NotNull final PsiFile psiFile,
                                        @NotNull final File tempDir)
            throws IOException {
        final File temporaryFile = new File(parentDirFor(psiFile, tempDir), psiFile.getName());
        temporaryFile.deleteOnExit();

        writeContentsToFile(psiFile, temporaryFile);

        return temporaryFile;
    }

    private File parentDirFor(@NotNull final PsiFile psiFile,
                              @NotNull final File baseTmpDir) {
        final File tmpDirForFile;

        if (psiFile instanceof PsiJavaFile) {
            final String packageName = ((PsiJavaFile) psiFile).getPackageName();
            final String packagePath = packageName.replaceAll("\\.", File.separator);

            tmpDirForFile = new File(baseTmpDir.getAbsolutePath() + File.separator + packagePath);

        } else {
            tmpDirForFile = baseTmpDir;
        }

        //noinspection ResultOfMethodCallIgnored
        tmpDirForFile.mkdirs();

        return tmpDirForFile;
    }

    private File prepareBaseTmpDir() {
        final File baseTmpDir = new File(System.getProperty("java.io.tmpdir"),
                TEMPFILE_DIR_PREFIX + UUID.randomUUID().toString());
        baseTmpDir.deleteOnExit();
        return baseTmpDir;
    }

    private boolean existsOnFilesystem(@NotNull final PsiFile psiFile) {
        final VirtualFile virtualFile = psiFile.getVirtualFile();
        return virtualFile != null
                && LocalFileSystem.getInstance().exists(psiFile.getVirtualFile());
    }

    private boolean fileOpenOrUnsaved(@NotNull final PsiFile psiFile) {
        final VirtualFile virtualFile = psiFile.getVirtualFile();
        //noinspection SimplifiableIfStatement
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

    /**
     * Delete the file if appropriate.
     */
    public void deleteIfRequired() {
        if (baseTempDir != null
                && baseTempDir.getName().startsWith(TEMPFILE_DIR_PREFIX)) {
            delete(baseTempDir);
        }
    }

    private void delete(@NotNull final File file) {
        if (!file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                delete(child);
            }
        }

        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    public String getAbsolutePath() {
        return realFile.getAbsolutePath();
    }

    @Override
    public String toString() {
        return String.format("[ScannableFile: file=%s; temporary=%s]", realFile.toString(), baseTempDir != null);
    }
}
