package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;

public class ScanAllFilesInProjectTask extends ScanAllFilesTask {

    private final Project project;

    ScanAllFilesInProjectTask(@NotNull final Project project,
                              final ConfigurationLocation selectedOverride) {
        super(project, selectedOverride);
        this.project = project;
    }
    @Override
    protected VirtualFile[] files() {
        return ProjectRootManager.getInstance(project).getContentRoots();
    }
}
