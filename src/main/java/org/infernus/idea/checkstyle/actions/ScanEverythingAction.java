package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


class ScanEverythingAction implements Runnable {

    private final Project project;
    private final Module module;
    private final ConfigurationLocation selectedOverride;

    ScanEverythingAction(@NotNull final Project project, final ConfigurationLocation selectedOverride) {
        this.project = project;
        this.module = null;
        this.selectedOverride = selectedOverride;
    }

    ScanEverythingAction(@NotNull final Module module, final ConfigurationLocation selectedOverride) {
        this.project = module.getProject();
        this.module = module;
        this.selectedOverride = selectedOverride;
    }

    @Override
    public void run() {
        List<VirtualFile> filesToScan;
        if (module != null) {
            // all non-excluded files of a module
            final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
            filesToScan = flattenFiles(moduleRootManager.getContentRoots());
        } else {
            // all non-excluded files of the project
            filesToScan = flattenFiles(new VirtualFile[]{project.getBaseDir()});
        }
        project.getComponent(CheckStylePlugin.class).asyncScanFiles(filesToScan, selectedOverride);
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
