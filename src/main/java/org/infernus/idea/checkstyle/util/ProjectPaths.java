package org.infernus.idea.checkstyle.util;

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectPaths {

    /**
     * Anchored to {@link Project#getBasePath()} rather than {@link ProjectUtil#guessProjectDir}: the
     * platform's own automatic $PROJECT_DIR$ macro substitution (which runs unconditionally over this
     * plugin's persisted XML on every load/save, independently of {@link ProjectFilePaths}'s own
     * tokenise/detokenise) is anchored to the project's base path too. guessProjectDir()'s "normal" path
     * instead derives from every module's content roots in the workspace model; if any of them is an
     * ancestor of the true project directory (e.g. a composite-build/settings-root pseudo-module), that
     * silently diverges from the platform's anchor and corrupts persisted PROJECT_RELATIVE paths (#708).
     */
    @Nullable
    public VirtualFile projectPath(@NotNull final Project project) {
        final String basePath = project.getBasePath();
        if (basePath == null) {
            return null;
        }
        return LocalFileSystem.getInstance().findFileByPath(basePath);
    }

    @Nullable
    public VirtualFile modulePath(@NotNull final Module module) {
        final VirtualFile externalProjectPath = externalProjectPathOf(module);
        if (externalProjectPath != null) {
            return externalProjectPath;
        }
        return ProjectUtil.guessModuleDir(module);
    }

    /**
     * The directory the external build system considers this module's own, e.g. a Gradle subproject
     * or a Maven module. Gradle source-set modules ({@code root.sub.main}) have content roots named
     * for the source set rather than the module, so guessing from content roots picks the wrong
     * directory - or a build output directory - for them.
     *
     * @param module the module to find the directory of.
     * @return the external project directory, or null if the module isn't managed by an external build system.
     */
    @Nullable
    private VirtualFile externalProjectPathOf(@NotNull final Module module) {
        final String externalProjectPath = ExternalSystemApiUtil.getExternalProjectPath(module);
        if (externalProjectPath == null) {
            return null;
        }

        final VirtualFile externalProjectDir = LocalFileSystem.getInstance().findFileByPath(externalProjectPath);
        if (externalProjectDir != null && externalProjectDir.isDirectory()) {
            return externalProjectDir;
        }
        return null;
    }

}
