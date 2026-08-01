package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageView;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Action to scan the files behind the selected search results.
 */
public class ScanSelectedSearchResults extends ScanSearchResults {

    @Override
    protected List<VirtualFile> filesToScan(@NotNull final AnActionEvent event) {
        final Usage[] usages = event.getData(UsageView.USAGES_KEY);
        return usages != null ? filesFrom(Arrays.asList(usages)) : List.of();
    }
}
