package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

class ScanSourceRootsAction implements Runnable {
    private final Project project;
    private final VirtualFile[] sourceRoots;
    private final ConfigurationLocation selectedOverride;

    ScanSourceRootsAction(@NotNull final Project project,
                          @NotNull final VirtualFile[] sourceRoots,
                          final ConfigurationLocation selectedOverride) {
        this.project = project;
        this.sourceRoots = sourceRoots;
        this.selectedOverride = selectedOverride;
    }

    @Override
    public void run() {
        ServiceManager.getService(project, CheckStylePlugin.class)
                .asyncScanFiles(flattenFiles(sourceRoots), selectedOverride);
    }

    private List<VirtualFile> flattenFiles(final VirtualFile[] files) {
        final List<VirtualFile> flattened = new ArrayList<>();

        if (files != null) {
            for (final VirtualFile file : files) {
                flattened.add(file);

                if (file.getChildren() != null) {
                    flattened.addAll(flattenFiles(file.getChildren()));
                }
            }
        }

        return flattened;
    }
}
