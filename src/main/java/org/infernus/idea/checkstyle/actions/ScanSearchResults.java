package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.usages.Usage;
import com.intellij.usages.rules.UsageInFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Base class for actions that scan the files behind a set of search results.
 */
public abstract class ScanSearchResults extends BaseAction {

    static List<VirtualFile> filesFrom(@NotNull final Collection<Usage> usages) {
        return usages.stream()
                .filter(Usage::isValid)
                .filter(UsageInFile.class::isInstance)
                .map(usage -> ((UsageInFile) usage).getFile())
                .filter(Objects::nonNull)
                .filter(VirtualFile::isValid)
                .toList();
    }
}
