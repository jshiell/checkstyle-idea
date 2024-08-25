package org.infernus.idea.checkstyle.util;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
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
        return ProjectUtil.guessModuleDir(module);
    }

}
