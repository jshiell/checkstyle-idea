package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Resolves the nodes of the results tree back to the files they describe.
 */
final class ResultTreeFiles {

    private ResultTreeFiles() {
    }

    /**
     * The files described by a node and everything beneath it.
     *
     * @param node the node to walk, which may be null.
     * @return the files, in the order they were encountered.
     */
    @NotNull
    static Set<PsiFile> filesUnder(@Nullable final Object node) {
        final Set<PsiFile> files = new LinkedHashSet<>();
        collectFilesUnder(node, files);
        return files;
    }

    private static void collectFilesUnder(@Nullable final Object node, final Set<PsiFile> files) {
        if (!(node instanceof ToggleableTreeNode toggleableNode)) {
            return;
        }

        if (toggleableNode.getUserObject() instanceof ProblemResultTreeInfo problemInfo
                && problemInfo.getFile() != null) {
            files.add(problemInfo.getFile());
        }

        // all children, not the visible ones: a file hidden behind a severity filter would otherwise
        // never be re-scanned, and would silently go stale
        toggleableNode.getAllChildren().forEach(child -> collectFilesUnder(child, files));
    }
}
