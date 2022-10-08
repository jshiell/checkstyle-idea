package org.infernus.idea.checkstyle.checker;

import com.intellij.application.options.CodeStyle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.util.TempDirProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;


/**
 * A representation of a file able to be scanned.
 */
public class ScannableFile {
    private static final Logger LOG = Logger.getInstance(ScannableFile.class);

    private static final String TEMPFILE_DIR_PREFIX = "csi-";

    private static final AtomicInteger TEMP_FILE_SOURCE = new AtomicInteger();
    private static final int MAX_TEMP_FILE_SUFFIX = 999;

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
    public ScannableFile(@NotNull final PsiFile psiFile, @Nullable final Module module) throws IOException {
        this.psiFile = psiFile;

        if (!existsOnFilesystem(psiFile) || documentIsModifiedAndUnsaved(psiFile)) {
            baseTempDir = prepareBaseTmpDirFor(psiFile);
            realFile = createTemporaryFileFor(psiFile, module, baseTempDir);
        } else {
            baseTempDir = null;
            realFile = new File(pathOf(psiFile));
        }
    }

    public static List<ScannableFile> createAndValidate(@NotNull final Collection<PsiFile> psiFiles,
                                                        @NotNull final Project project,
                                                        @Nullable final Module module,
                                                        @Nullable final ConfigurationLocation overrideConfigLocation) {
        Computable<List<ScannableFile>> action = () -> psiFiles.stream()
                .filter(currentFile -> PsiFileValidator.isScannable(
                        currentFile,
                        ofNullable(module),
                        configurationManager(project),
                        overrideConfigLocation))
                .map(currentFile -> ScannableFile.create(currentFile, module))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(CopyOnWriteArrayList::new));
        return ApplicationManager.getApplication().runReadAction(action);
    }

    private static PluginConfigurationManager configurationManager(final Project project) {
        return project.getService(PluginConfigurationManager.class);
    }

    @Nullable
    private static ScannableFile create(@NotNull final PsiFile psiFile, @Nullable final Module module) {
        try {
            final CreateScannableFileAction fileAction = new CreateScannableFileAction(psiFile, module);
            ApplicationManager.getApplication().runReadAction(fileAction);

            if (fileAction.getFailure() != null) {
                throw fileAction.getFailure();
            }

            return fileAction.getFile();
        } catch (IOException e) {
            LOG.warn("Failure when creating temporary file", e);
            return null;
        }
    }

    private String pathOf(@NotNull final PsiFile file) {
        return virtualFileOf(file)
                .map(VirtualFile::getPath)
                .orElseThrow(() -> new IllegalStateException("PSIFile " + "does not have associated virtual file: " + file));
    }

    private File createTemporaryFileFor(@NotNull final PsiFile file,
                                        @Nullable final Module module,
                                        @NotNull final File tempDir) throws IOException {
        final File temporaryFile = new File(parentDirFor(file, module, tempDir), file.getName());
        temporaryFile.deleteOnExit();

        writeContentsToFile(file, temporaryFile);

        return temporaryFile;
    }

    private File parentDirFor(@NotNull final PsiFile file,
                              @Nullable final Module module,
                              @NotNull final File baseTmpDir) {
        File tmpDirForFile = relativePathToProjectRoot(file, baseTmpDir);

        if (tmpDirForFile == null && module != null) {
            tmpDirForFile = relativePathToModuleContentRoots(file, module, baseTmpDir);
        }

        if (tmpDirForFile == null && file instanceof PsiJavaFile) {
            tmpDirForFile = classPackagePath((PsiJavaFile) file, baseTmpDir);
        }

        if (tmpDirForFile == null) {
            tmpDirForFile = baseTmpDir;
        }

        //noinspection ResultOfMethodCallIgnored
        tmpDirForFile.mkdirs();

        return tmpDirForFile;
    }

    @NotNull
    private File classPackagePath(final @NotNull PsiJavaFile file, final @NotNull File baseTmpDir) {
        final String packagePath = file.getPackageName().replaceAll("\\.", Matcher.quoteReplacement(File.separator));

        return new File(baseTmpDir.getAbsolutePath() + File.separator + packagePath);
    }

    private File relativePathToProjectRoot(final @NotNull PsiFile file, final @NotNull File baseTmpDir) {
        if (file.getParent() != null) {
            final VirtualFile projectDir = ProjectUtil.guessProjectDir(file.getProject());
            if (projectDir != null) {
                final String baseDirUrl = projectDir.getUrl();

                final String parentUrl = file.getParent().getVirtualFile().getUrl();
                if (parentUrl.startsWith(baseDirUrl)) {
                    return new File(baseTmpDir.getAbsolutePath() + parentUrl.substring(baseDirUrl.length()));
                }
            }
        }
        return null;
    }

    private File relativePathToModuleContentRoots(final @NotNull PsiFile file, final @NotNull Module module, final
    @NotNull File baseTmpDir) {
        if (file.getParent() != null) {
            final String parentUrl = file.getParent().getVirtualFile().getUrl();
            for (String moduleSourceRoot : ModuleRootManager.getInstance(module).getContentRootUrls()) {
                if (parentUrl.startsWith(moduleSourceRoot)) {
                    return new File(baseTmpDir.getAbsolutePath() + parentUrl.substring(moduleSourceRoot.length()));
                }
            }
        }
        return null;
    }

    private File prepareBaseTmpDirFor(final PsiFile tempPsiFile) {
        final File baseTmpDir = new File(new TempDirProvider().forPersistedPsiFile(tempPsiFile), tempFileDirectoryName());
        baseTmpDir.deleteOnExit();
        return baseTmpDir;
    }

    private String tempFileDirectoryName() {
        return String.format("%s%03d", TEMPFILE_DIR_PREFIX, TEMP_FILE_SOURCE.getAndUpdate(incrementUntil(MAX_TEMP_FILE_SUFFIX)));
    }

    @NotNull
    private IntUnaryOperator incrementUntil(final int maxValue) {
        return operand -> {
            if (operand >= maxValue) {
                return 0;
            }
            return operand + 1;
        };
    }

    private boolean existsOnFilesystem(@NotNull final PsiFile file) {
        return virtualFileOf(file).map(virtualFile -> LocalFileSystem.getInstance().exists(virtualFile)).orElse(false);
    }

    private boolean documentIsModifiedAndUnsaved(final PsiFile file) {
        final FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        return virtualFileOf(file).filter(fileDocumentManager::isFileModified).map(fileDocumentManager::getDocument)
                .map(fileDocumentManager::isDocumentUnsaved).orElse(false);
    }

    private void writeContentsToFile(final PsiFile file, final File outFile) throws IOException {
        final String lineSeparator = CodeStyle.getSettings(file.getProject()).getLineSeparator();

        final Writer tempFileOut = writerTo(outFile, charSetOf(file));
        for (final char character : file.getText().toCharArray()) {
            if (character == '\n') { // IDEA uses \n internally
                tempFileOut.write(lineSeparator);
            } else {
                tempFileOut.write(character);
            }
        }
        tempFileOut.flush();
        tempFileOut.close();
    }

    @NotNull
    private Charset charSetOf(final PsiFile file) {
        return virtualFileOf(file).map(VirtualFile::getCharset).orElse(StandardCharsets.UTF_8);
    }

    private Optional<VirtualFile> virtualFileOf(final PsiFile file) {
        return ofNullable(file.getVirtualFile());
    }

    @NotNull
    private Writer writerTo(final File outFile, final Charset charset) throws FileNotFoundException {
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), charset.newEncoder()));
    }

    public File getFile() {
        return realFile;
    }

    public static void deleteIfRequired(@Nullable final ScannableFile scannableFile) {
        if (scannableFile != null) {
            scannableFile.deleteIfRequired();
        }
    }

    private void deleteIfRequired() {
        if (baseTempDir != null && baseTempDir.getName().startsWith(TEMPFILE_DIR_PREFIX)) {
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
