package org.infernus.idea.checkstyle.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.function.Function;

public class ProjectFilePaths {

    private static final Logger LOG = Logger.getInstance(ProjectFilePaths.class);

    static final String PROJECT_DIR_TOKEN = "$PROJECT_DIR$";

    private final ProjectPaths projectPaths;
    private final Project project;
    private final char separatorChar;
    private final Function<File, String> absolutePathOf;

    public ProjectFilePaths(@NotNull final Project project) {
        this(project, File.separatorChar, File::getAbsolutePath, project.getService(ProjectPaths.class));
    }

    private ProjectFilePaths(@NotNull final Project project,
                             final char separatorChar,
                             @NotNull final Function<File, String> absolutePathOf,
                             @NotNull final ProjectPaths projectPaths) {
        this.project = project;
        this.separatorChar = separatorChar;
        this.absolutePathOf = absolutePathOf;

        this.projectPaths = projectPaths;
    }

    public static ProjectFilePaths testInstanceWith(@NotNull final Project project,
                                                    @NotNull final ProjectPaths projectPaths) {
        return new ProjectFilePaths(project, File.separatorChar, File::getAbsolutePath, projectPaths);
    }

    public static ProjectFilePaths testInstanceWith(@NotNull final Project project,
                                                    final char separatorChar,
                                                    @NotNull final Function<File, String> absolutePathOf,
                                                    @NotNull final ProjectPaths projectPaths) {
        return new ProjectFilePaths(project, separatorChar, absolutePathOf, projectPaths);
    }

    @Nullable
    public String makeProjectRelative(@Nullable final String path) {
        if (path == null || project.isDefault()) {
            return path;
        }

        final File projectPath = projectPath();
        if (projectPath == null) {
            LOG.debug("Couldn't find project path, returning full path: " + path);
            return path;
        }

        try {
            final String basePath = toUnix(absolutePathOf.apply(projectPath)) + '/';
            return FilePaths.relativePath(toUnix(path), basePath, "/");

        } catch (FilePaths.PathResolutionException e) {
            LOG.debug("No common path was found between " + path + " and " + projectPath.getAbsolutePath());
            return path;

        } catch (Exception e) {
            LOG.warn("Failed to make relative: " + path, e);
            return path;
        }
    }

    @Nullable
    public String tokenise(@Nullable final String fsPath) {
        if (fsPath == null) {
            return null;
        }

        final String basePath = projectBasePath();
        if (basePath == null) {
            return fsPath;
        }

        final String unixPath = toUnix(fsPath);

        // Already tokenised — return unchanged (idempotent)
        if (unixPath.startsWith(PROJECT_DIR_TOKEN)) {
            return fsPath;
        }

        // Absolute path rooted at the project directory
        if (unixPath.startsWith(basePath + '/')) {
            return PROJECT_DIR_TOKEN + unixPath.substring(basePath.length());
        }

        // Relative path returned by makeProjectRelative — prepend the token
        if (!isAbsolute(unixPath)) {
            return PROJECT_DIR_TOKEN + '/' + unixPath;
        }

        return fsPath;
    }

    @Nullable
    public String detokenise(@Nullable final String tokenisedPath) {
        if (tokenisedPath == null || !tokenisedPath.startsWith(PROJECT_DIR_TOKEN)) {
            return tokenisedPath;
        }

        final String basePath = projectBasePath();
        if (basePath == null) {
            return tokenisedPath;
        }

        return basePath + tokenisedPath.substring(PROJECT_DIR_TOKEN.length());
    }

    @Nullable
    private String projectBasePath() {
        final File pp = projectPath();
        if (pp == null) {
            return null;
        }
        return toUnix(absolutePathOf.apply(pp));
    }

    private static String toUnix(@NotNull final String path) {
        return path.replace('\\', '/');
    }

    private static boolean isAbsolute(@NotNull final String unixPath) {
        // Unix absolute: starts with /
        // Windows absolute: starts with drive letter e.g. c:/
        return unixPath.startsWith("/") || (unixPath.length() > 2 && unixPath.charAt(1) == ':');
    }

    @Nullable
    private File projectPath() {
        try {
            final VirtualFile baseDir = projectPaths.projectPath(project);
            if (baseDir == null) {
                return null;
            }

            return new File(baseDir.getPath());

        } catch (Exception e) {
            // IDEA 10.5.2 sometimes throws an AssertionException in project.getBaseDir()
            LOG.debug("Couldn't retrieve base location", e);
            return null;
        }
    }
}
