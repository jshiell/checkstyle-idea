package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;

class ScanAllGivenFilesTask extends ScanAllFilesTask {
    private final VirtualFile[] filesToScan;

    ScanAllGivenFilesTask(@NotNull final Project project,
                          @NotNull final VirtualFile[] filesToScan,
                          final ConfigurationLocation selectedOverride) {
        super(project, selectedOverride);
        this.filesToScan = filesToScan;
    }

    @Override
    protected VirtualFile[] files() {
        return filesToScan;
    }
}
