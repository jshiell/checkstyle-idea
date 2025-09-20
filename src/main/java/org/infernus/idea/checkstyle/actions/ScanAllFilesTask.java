package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import org.infernus.idea.checkstyle.StaticScanner;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;


abstract class ScanAllFilesTask implements Callable<Void> {

    private final Project project;
    private final ConfigurationLocation selectedOverride;

    ScanAllFilesTask(@NotNull final Project project,
                     final ConfigurationLocation selectedOverride) {
        this.project = project;
        this.selectedOverride = selectedOverride;
    }

    @Override
    public Void call() {
        project.getService(StaticScanner.class)
                .asyncScanFiles(flattenFiles(files()), selectedOverride);
        return null;
    }

    protected abstract VirtualFile[] files();

    private List<VirtualFile> flattenFiles(final VirtualFile[] files) {
        final List<VirtualFile> flattened = new ArrayList<>();
        if (files != null) {
            for (final VirtualFile file : files) {
                flattened.add(file);
                flattened.addAll(ReadAction.compute(() -> {
                    final List<VirtualFile> flattenedChildren = new ArrayList<>();
                    VfsUtilCore.visitChildrenRecursively(file, new VirtualFileVisitor<>() {
                        @Override
                        @NotNull
                        public Result visitFileEx(@NotNull final VirtualFile file) {
                            flattenedChildren.add(file);
                            return CONTINUE;
                        }
                    });
                    return flattenedChildren;
                }));
            }
        }
        return flattened;
    }
}
