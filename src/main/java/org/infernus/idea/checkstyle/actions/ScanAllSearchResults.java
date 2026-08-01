package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageView;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Action to scan every file in the current search results.
 */
public class ScanAllSearchResults extends ScanSearchResults {

    @Override
    protected List<VirtualFile> filesToScan(@NotNull final AnActionEvent event) {
        final UsageView usageView = event.getData(UsageView.USAGE_VIEW_KEY);
        return usageView != null ? filesOfAllResultsIn(usageView) : List.of();
    }

    static List<VirtualFile> filesOfAllResultsIn(@NotNull final UsageView usageView) {
        // getUsages() is a live view over the view's usages, and includes those the user has excluded
        final Set<Usage> includedUsages = new LinkedHashSet<>(usageView.getUsages());
        includedUsages.removeAll(usageView.getExcludedUsages());
        return filesFrom(includedUsages);
    }
}
