package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;

public class ScanAllFilesInModuleTask extends ScanAllFilesTask {

    private final Module module;

    ScanAllFilesInModuleTask(@NotNull final Module module,
                             final ConfigurationLocation selectedOverride) {
        super(module.getProject(), selectedOverride);
        this.module = module;
    }

    @Override
    protected VirtualFile[] files() {
        return ModuleRootManager.getInstance(module).getContentRoots();
    }
}
