package org.infernus.idea.checkstyle.toolwindow;

import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.Serial;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tree node with toggleable visibility.
 */
public class ToggleableTreeNode extends DefaultMutableTreeNode {
    @Serial
    private static final long serialVersionUID = -4490734768175672868L;

    private boolean visible = true;

    public ToggleableTreeNode() {
    }

    public ToggleableTreeNode(final Object userObject) {
        super(userObject);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(final boolean visible) {
        this.visible = visible;
    }

    @NotNull
    List<ToggleableTreeNode> getAllChildren() {
        if (children != null) {
            return children.stream()
                    .map(child -> (ToggleableTreeNode) child)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public void removeAllChildren() {
        if (children != null) {
            children.clear();
        }
    }

    @Override
    public TreeNode getChildAt(final int index) {
        if (children == null) {
            throw new ArrayIndexOutOfBoundsException("Invalid index: " + index + " (no children)");
        }

        int realIndex = -1;
        int visibleIndex = -1;

        for (final Object child : children) {
            final ToggleableTreeNode node = (ToggleableTreeNode) child;
            if (node.isVisible()) {
                ++visibleIndex;
            }
            ++realIndex;
            if (visibleIndex == index) {
                return children.get(realIndex);
            }
        }

        throw new ArrayIndexOutOfBoundsException("Invalid index: " + index);
    }

    @Override
    public int getChildCount() {
        if (children == null) {
            return 0;
        }

        int count = 0;
        for (final Object child : children) {
            final ToggleableTreeNode node = (ToggleableTreeNode) child;
            if (node.isVisible()) {
                ++count;
            }
        }

        return count;
    }
}
