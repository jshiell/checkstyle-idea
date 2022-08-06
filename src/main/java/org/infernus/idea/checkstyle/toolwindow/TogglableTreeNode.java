package org.infernus.idea.checkstyle.toolwindow;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tree node with togglable visibility.
 */
public class TogglableTreeNode extends DefaultMutableTreeNode {
    private static final long serialVersionUID = -4490734768175672868L;

    private boolean visible = true;

    public TogglableTreeNode() {
    }

    public TogglableTreeNode(final Object userObject) {
        super(userObject);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(final boolean visible) {
        this.visible = visible;
    }

    List<TogglableTreeNode> getAllChildren() {
        return children.stream()
                .map(child -> (TogglableTreeNode) child)
                .collect(Collectors.toList());
    }

    @Override
    public TreeNode getChildAt(final int index) {
        int realIndex = -1;
        int visibleIndex = -1;

        for (final Object child : children) {
            final TogglableTreeNode node = (TogglableTreeNode) child;
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
            final TogglableTreeNode node = (TogglableTreeNode) child;
            if (node.isVisible()) {
                ++count;
            }
        }

        return count;
    }
}
