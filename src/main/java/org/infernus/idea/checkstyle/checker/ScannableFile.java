package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static org.infernus.idea.checkstyle.checker.PsiFileValidator.isScannable;

/**
 * A representation of a file able to be scanned.
 */
public class ScannableFile {
    private static final Logger LOG = LoggerFactory.getLogger(ScannableFile.class);

    private static final String TEMPFILE_DIR_PREFIX = "csi-";

    private final File realFile;
    private final File baseTempDir;
    private final PsiFile psiFile;

    /**
     * Create a new scannable file from a PSI file.
     * <p>
     * If required this will create a temporary copy of the file.
     *
     * @param psiFile the psiFile to create the file from.
     * @param module  the module the file belongs to.
     * @throws IOException if file creation is required and fails.
     */
    public ScannableFile(@NotNull final PsiFile psiFile,
                         @NotNull final Module module)
            throws IOException {
        this.psiFile = psiFile;

        if (!existsOnFilesystem(psiFile)
                || documentIsModifiedAndUnsaved(psiFile.getVirtualFile())) {
            baseTempDir = prepareBaseTmpDir();
            realFile = createTemporaryFileFor(psiFile, module, baseTempDir);

        } else {
            baseTempDir = null;
            realFile = new File(pathOf(psiFile));
        }
    }

    public static List<ScannableFile> createAndValidate(@NotNull final Collection<PsiFile> psiFiles,
                                                        @NotNull final CheckStylePlugin plugin,
                                                        @NotNull final Module module) {

        final AccessToken readAccessToken = ApplicationManager.getApplication().acquireReadActionLock();
        try {
            return psiFiles.stream()
                    .filter(psiFile -> isScannable(psiFile, Optional.ofNullable(module), plugin.getConfiguration()))
                    .map(psiFile -> ScannableFile.create(psiFile, module))
                    .filter(psiFile -> psiFile != null)
                    .collect(Collectors.toList());

        } finally {
            readAccessToken.finish();
        }
    }

    @Nullable
    private static ScannableFile create(@NotNull final PsiFile psiFile,
                                        @NotNull final Module module) {
        try {
            final CreateScannableFileAction fileAction = new CreateScannableFileAction(psiFile, module);
            ApplicationManager.getApplication().runReadAction(fileAction);

            //noinspection ThrowableResultOfMethodCallIgnored
            if (fileAction.getFailure() != null) {
                throw fileAction.getFailure();
            }

            return fileAction.getFile();

        } catch (IOException e) {
            LOG.error("Failure when creating temporary file", e);
            return null;
        }
    }

    private String pathOf(@NotNull final PsiFile file) {
        if (file.getVirtualFile() != null) {
            return file.getVirtualFile().getPath();
        }
        throw new IllegalStateException("PSIFile does not have associated virtual file: " + file);
    }

    private File createTemporaryFileFor(@NotNull final PsiFile file,
                                        @NotNull final Module module,
                                        @NotNull final File tempDir)
            throws IOException {
        final File temporaryFile = new File(parentDirFor(file, module, tempDir), file.getName());
        temporaryFile.deleteOnExit();

        writeContentsToFile(file, temporaryFile);

        return temporaryFile;
    }

    private File parentDirFor(@NotNull final PsiFile file,
                              @NotNull final Module module,
                              @NotNull final File baseTmpDir) {
        File tmpDirForFile = null;

        if (file.getParent() != null) {
            final String parentUrl = file.getParent().getVirtualFile().getUrl();
            for (String moduleSourceRoot : ModuleRootManager.getInstance(module).getContentRootUrls()) {
                if (parentUrl.startsWith(moduleSourceRoot)) {
                    tmpDirForFile = new File(baseTmpDir.getAbsolutePath() + parentUrl.substring(moduleSourceRoot.length()));
                    break;
                }
            }
        }

        if (tmpDirForFile == null) {
            if (file instanceof PsiJavaFile) {
                final String packageName = ((PsiJavaFile) file).getPackageName();
                final String packagePath = packageName.replaceAll(
                        "\\.", Matcher.quoteReplacement(File.separator));

                tmpDirForFile = new File(baseTmpDir.getAbsolutePath() + File.separator + packagePath);
            } else {
                tmpDirForFile = baseTmpDir;
            }
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

    private boolean existsOnFilesystem(@NotNull final PsiFile file) {
        final VirtualFile virtualFile = file.getVirtualFile();
        return virtualFile != null
                && LocalFileSystem.getInstance().exists(virtualFile);
    }

    private boolean documentIsModifiedAndUnsaved(final VirtualFile virtualFile) {
        if (virtualFile == null) {
            return false;
        }
        final FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        if (fileDocumentManager.isFileModified(virtualFile)) {
            final Document document = fileDocumentManager.getDocument(virtualFile);
            if (document != null) {
                return fileDocumentManager.isDocumentUnsaved(document);
            }
        }
        return false;
    }

    private void writeContentsToFile(final PsiFile file,
                                     final File outFile)
            throws IOException {
        final CodeStyleSettings codeStyleSettings
                = CodeStyleSettingsManager.getSettings(file.getProject());

        final Writer tempFileOut = writerTo(outFile);
        for (final char character : file.getText().toCharArray()) {
            if (character == '\n') { // IDEA uses \n internally
                tempFileOut.write(codeStyleSettings.getLineSeparator());
            } else {
                tempFileOut.write(character);
            }
        }
        tempFileOut.flush();
        tempFileOut.close();
    }

    @NotNull
    private Writer writerTo(final File outFile) throws FileNotFoundException {
        return new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(outFile), Charset.forName("UTF-8").newEncoder()));
    }

    public File getFile() {
        return realFile;
    }

    public static void deleteIfRequired(ScannableFile scannableFile) {
        if (scannableFile != null) {
            scannableFile.deleteIfRequired();
        }
    }

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
            final File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    delete(child);
                }
            }
        }

        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    public String getAbsolutePath() {
        return realFile.getAbsolutePath();
    }

    public PsiFile getPsiFile() {
        return psiFile;
    }

    @Override
    public String toString() {
        return String.format("[ScannableFile: file=%s; temporary=%s]", realFile.toString(), baseTempDir != null);
    }
}
