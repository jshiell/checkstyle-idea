package org.infernus.idea.checkstyle.util;

import com.intellij.openapi.components.ServiceKt;
import com.intellij.openapi.components.StorageScheme;
import com.intellij.openapi.components.impl.stores.IProjectStore;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;


/**
 * Locate and/or create temporary directories for use by this plugin.
 */
public class TempDirProvider {
    private static final Logger LOG = Logger.getInstance(TempDirProvider.class);

    private static final String README_TEMPLATE = "copylibs-readme.template.txt";

    public static final String README_FILE = "readme.txt";


    public String forPersistedPsiFile(final PsiFile tempPsiFile) {
        String systemTempDir = System.getProperty("java.io.tmpdir");
        if (OS.isWindows() && driveLetterOf(systemTempDir) != driveLetterOf(pathOf(tempPsiFile))) {
            // Checkstyle on Windows requires the files to be on the same drive
            final File projectTempDir = temporaryDirectoryLocationFor(tempPsiFile.getProject());
            if (projectTempDir.exists() || projectTempDir.mkdirs()) {
                projectTempDir.deleteOnExit();
                return projectTempDir.getAbsolutePath();
            }
        }
        return systemTempDir;
    }

    @NotNull
    private File temporaryDirectoryLocationFor(final Project project) {
        return getIdeaFolder(project).map(vf -> new File(vf.getPath(), "checkstyleidea.tmp"))
                .orElse(new File(project.getBasePath(), "checkstyleidea.tmp"));
    }

    Optional<VirtualFile> getIdeaFolder(@NotNull final Project project) {
        final IProjectStore projectStore = (IProjectStore) ServiceKt.getStateStore(project);
        if (projectStore.getStorageScheme() == StorageScheme.DIRECTORY_BASED) {
            VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
            if (projectDir != null) {
                final VirtualFile ideaStorageDir = projectDir.findChild(Project.DIRECTORY_STORE_FOLDER);
                if (ideaStorageDir != null && ideaStorageDir.exists() && ideaStorageDir.isDirectory()) {
                    return Optional.of(ideaStorageDir);
                }
            }
        }
        return Optional.empty();
    }

    private char driveLetterOf(final String windowsPath) {
        if (windowsPath != null && windowsPath.length() > 0) {
            final Path normalisedPath = Paths.get(windowsPath).normalize().toAbsolutePath();
            return normalisedPath.toFile().toString().charAt(0);
        }
        return '?';
    }

    private String pathOf(@NotNull final PsiFile file) {
        return virtualFileOf(file).map(VirtualFile::getPath).orElseThrow(() ->
                new IllegalStateException("PSIFile does not have associated virtual file: " + file));
    }

    private Optional<VirtualFile> virtualFileOf(final PsiFile file) {
        return Optional.ofNullable(file.getVirtualFile());
    }


    /**
     * Locate the directory for storing libraries from the project directory in order to prevent their getting locked
     * by our classloaders. The directory will be created if it does not exist. It should persist after IDEA is closed,
     * because copying the libraries is potentially time-consuming (so, no deleteOnExit()!).
     *
     * @param pProject the current project
     * @return the existing directory, or an empty Optional if such could not be made available
     */
    public Optional<File> forCopiedLibraries(@NotNull final Project pProject) {
        Optional<File> result = Optional.empty();
        try {
            final File tempDir = determineCopiedLibrariesDir(pProject);
            if (tempDir.mkdir()) {
                putReadmeFile(pProject, tempDir);
            }
            if (tempDir.isDirectory()) {
                result = Optional.of(tempDir);
            }
        } catch (IOException | RuntimeException e) {
            LOG.warn("Unable to create suitable temporary directory. Library unlock unavailable.", e);
        }
        return result;
    }

    @NotNull
    private File determineCopiedLibrariesDir(@NotNull final Project pProject) {
        return getIdeaFolder(pProject).map(pVirtualFile -> new File(pVirtualFile.getPath(),
                "checkstyleidea-libs")).orElseGet(() -> new File(System.getProperty("java.io.tmpdir"),
                "csi-" + projectUnique(pProject) + "-libs"));
    }

    @NotNull
    private String projectUnique(@NotNull final Project pProject) {
        return pProject.getLocationHash().replaceAll(" ", "_");
    }

    private void putReadmeFile(@NotNull final Project project, @NotNull final File pTempDir)
            throws IOException {
        final Path tempDir = Paths.get(pTempDir.toURI());
        final Path readme = tempDir.resolve(README_FILE);
        if (!Files.isRegularFile(readme)) {
            String template = readTemplate();
            if (template != null) {
                template = MessageFormat.format(template, project.getName(), CheckStylePlugin.ID_PLUGIN);
                template = template.replaceAll("[\r\n]+", System.lineSeparator());
                Files.write(readme, template.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
            }
        }
    }

    @Nullable
    private String readTemplate() throws IOException {
        try (InputStream is = getClass().getResourceAsStream(README_TEMPLATE)) {
            return IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
        }
    }


    public void deleteCopiedLibrariesDir(@NotNull final Project pProject) {
        try {
            final File dir = determineCopiedLibrariesDir(pProject);
            if (dir.isDirectory()) {
                FileUtils.deleteQuietly(dir);
            }
        } catch (RuntimeException e) {
            // ignore
        }
    }
}
