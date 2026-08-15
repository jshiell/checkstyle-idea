package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
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

    /**
     * The nodes representing any of the passed files, so that a selection can be restored after the
     * tree has been rebuilt.
     *
     * @param node  the node to walk, which may be null.
     * @param files the files of interest.
     * @return the file nodes showing any of those files, which under some groupings is more than one
     * node per file.
     */
    @NotNull
    static List<ToggleableTreeNode> fileNodesShowing(@Nullable final Object node,
                                                     @NotNull final Set<PsiFile> files) {
        final List<ToggleableTreeNode> fileNodes = new ArrayList<>();
        collectFileNodesShowing(node, files, fileNodes);
        return fileNodes;
    }

    private static void collectFileNodesShowing(@Nullable final Object node,
                                                final Set<PsiFile> files,
                                                final List<ToggleableTreeNode> fileNodes) {
        if (!(node instanceof ToggleableTreeNode toggleableNode)) {
            return;
        }

        if (toggleableNode.getUserObject() instanceof FileGroupTreeInfo
                && !Collections.disjoint(filesUnder(toggleableNode), files)) {
            fileNodes.add(toggleableNode);
        }

        toggleableNode.getAllChildren().forEach(child -> collectFileNodesShowing(child, files, fileNodes));
    }
}
