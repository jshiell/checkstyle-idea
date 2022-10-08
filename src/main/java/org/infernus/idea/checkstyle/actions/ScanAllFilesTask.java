package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import org.infernus.idea.checkstyle.StaticScanner;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


abstract class ScanAllFilesTask implements Runnable {

    private final Project project;
    private final ConfigurationLocation selectedOverride;

    ScanAllFilesTask(@NotNull final Project project,
                     final ConfigurationLocation selectedOverride) {
        this.project = project;
        this.selectedOverride = selectedOverride;
    }

    @Override
    public void run() {
        project.getService(StaticScanner.class)
                .asyncScanFiles(flattenFiles(files()), selectedOverride);
    }

    protected abstract VirtualFile[] files();

    private List<VirtualFile> flattenFiles(final VirtualFile[] files) {
        final List<VirtualFile> flattened = new ArrayList<>();
        if (files != null) {
            for (final VirtualFile file : files) {
                flattened.add(file);
                VfsUtilCore.visitChildrenRecursively(file, new VirtualFileVisitor<>() {
                    @Override
                    @NotNull
                    public Result visitFileEx(@NotNull final VirtualFile file) {
                        flattened.add(file);
                        return CONTINUE;
                    }
                });
            }
        }
        return flattened;
    }
}
