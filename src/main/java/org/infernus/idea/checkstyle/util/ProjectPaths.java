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

    @Nullable
    public VirtualFile projectPath(@NotNull final Project project) {
        // workaround to allow testing with Jetbrain's love of static shite
        return ProjectUtil.guessProjectDir(project);
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
