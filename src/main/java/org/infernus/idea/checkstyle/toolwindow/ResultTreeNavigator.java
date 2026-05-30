package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import static org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel.Direction.FORWARD;

/**
 * Handles tree navigation: next/previous leaf selection and source scrolling.
 */
public class ResultTreeNavigator {

    private final Project project;
    private final JTree resultsTree;

    public ResultTreeNavigator(@NotNull final Project project,
                               @NotNull final JTree resultsTree) {
        this.project = project;
        this.resultsTree = resultsTree;
    }

    public void jumpToSource() {
        final TreePath treePath = resultsTree.getSelectionPath();
        if (treePath != null) {
            scrollToError(treePath, true);
        }
    }

    public void selectPreviousResult() {
        final TreePath previousResultPath = findPreviousLeaf(resultsTree.getSelectionPath());
        if (previousResultPath != null) {
            resultsTree.setSelectionPath(previousResultPath);
            resultsTree.scrollPathToVisible(previousResultPath);
        }
    }

    public void selectNextResult() {
        final TreePath nextResultPath = findNextLeaf(resultsTree.getSelectionPath());
        if (nextResultPath != null) {
            resultsTree.setSelectionPath(nextResultPath);
            resultsTree.scrollPathToVisible(nextResultPath);
        }
    }

    public void collapseTree() {
        for (int i = 1; i < resultsTree.getRowCount(); ++i) {
            resultsTree.collapseRow(i);
        }
    }

    public void expandTree(final ResultTreeModel treeModel, final int level) {
        expandNode(treeModel.getVisibleRoot(),
                new TreePath(treeModel.getPathToRoot(treeModel.getVisibleRoot())), level);
    }

    private void expandNode(final TreeNode node, final TreePath path, final int level) {
        if (level <= 0) {
            return;
        }
        resultsTree.expandPath(path);
        for (int i = 0; i < node.getChildCount(); ++i) {
            final TreeNode childNode = node.getChildAt(i);
            expandNode(childNode, path.pathByAddingChild(childNode), level - 1);
        }
    }

    /**
     * Scroll to the error specified by the given tree path.
     *
     * @param treePath    the tree path to scroll to.
     * @param focusEditor whether to focus the editor.
     */
    public void scrollToError(final TreePath treePath, final boolean focusEditor) {
        final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) treePath.getLastPathComponent();
        if (treeNode == null || !(treeNode.getUserObject() instanceof ProblemResultTreeInfo nodeInfo)) {
            return;
        }
        if (nodeInfo.getFile() == null || nodeInfo.getProblem() == null) {
            return;
        }
        final VirtualFile virtualFile = nodeInfo.getFile().getVirtualFile();
        if (virtualFile == null || !virtualFile.exists()) {
            return;
        }
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        ApplicationManager.getApplication().invokeLater(() -> {
            final FileEditor[] editor = fileEditorManager.openFile(virtualFile, focusEditor);
            if (editor.length > 0 && editor[0] instanceof TextEditor) {
                final LogicalPosition problemPos = new LogicalPosition(
                        Math.max(nodeInfo.getProblem().line() - 1, 0),
                        Math.max(nodeInfo.getProblem().column(), 0));
                final Editor textEditor = ((TextEditor) editor[0]).getEditor();
                textEditor.getCaretModel().moveToLogicalPosition(problemPos);
                textEditor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
            }
        }, ModalityState.nonModal());
    }

    @Nullable
    private TreePath findPreviousLeaf(@Nullable final TreePath startingPath) {
        if (startingPath == null) {
            return null;
        }
        return findSubsequentLeaf(startingPath, CheckStyleToolWindowPanel.Direction.BACKWARD);
    }

    @Nullable
    private TreePath findNextLeaf(@Nullable final TreePath startingPath) {
        if (startingPath == null) {
            return null;
        }
        final TreeModel model = resultsTree.getModel();
        final Object node = startingPath.getLastPathComponent();
        if (!model.isLeaf(node)) {
            TreePath next = startingPath.pathByAddingChild(model.getChild(node, 0));
            while (!model.isLeaf(next.getLastPathComponent())) {
                next = next.pathByAddingChild(model.getChild(next.getLastPathComponent(), 0));
            }
            return next;
        }
        return findSubsequentLeaf(startingPath, FORWARD);
    }

    @Nullable
    private TreePath findSubsequentLeaf(@NotNull final TreePath startingPath,
                                        final CheckStyleToolWindowPanel.Direction direction) {
        final TreeModel model = resultsTree.getModel();
        TreePath currentPath = startingPath;
        TreePath parentPath = currentPath.getParentPath();
        while (parentPath != null) {
            final Object parent = parentPath.getLastPathComponent();
            final int childIndex = model.getIndexOfChild(parent, currentPath.getLastPathComponent());
            final int adjacentChildIndex = direction == FORWARD ? childIndex + 1 : childIndex - 1;
            if (adjacentChildIndex >= 0 && adjacentChildIndex < model.getChildCount(parent)) {
                final Object nextSibling = model.getChild(parent, adjacentChildIndex);
                TreePath nextSiblingPath = parentPath.pathByAddingChild(nextSibling);
                while (!model.isLeaf(nextSiblingPath.getLastPathComponent())) {
                    final int startingChildIndex = direction == FORWARD
                            ? 0
                            : model.getChildCount(nextSiblingPath.getLastPathComponent()) - 1;
                    nextSiblingPath = nextSiblingPath.pathByAddingChild(
                            model.getChild(nextSiblingPath.getLastPathComponent(), startingChildIndex));
                }
                return nextSiblingPath;
            }
            currentPath = parentPath;
            parentPath = currentPath.getParentPath();
        }
        return null;
    }
}
