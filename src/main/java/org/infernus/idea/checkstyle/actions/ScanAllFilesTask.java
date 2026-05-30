package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.infernus.idea.checkstyle.StaticScanner;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
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
                .asyncScanFiles(rootFiles(), selectedOverride);
        return null;
    }

    protected abstract VirtualFile[] files();

    private List<VirtualFile> rootFiles() {
        final VirtualFile[] files = files();
        if (files == null) {
            return Collections.emptyList();
        }
        // Pass root VirtualFiles directly; ScanFiles.buildFilesList() handles recursive traversal
        // via VfsUtilCore.visitChildrenRecursively, so pre-flattening here is redundant.
        return Arrays.asList(files);
    }
}
