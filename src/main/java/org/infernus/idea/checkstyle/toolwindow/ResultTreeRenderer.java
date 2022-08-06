package org.infernus.idea.checkstyle.toolwindow;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

/**
 * The cell renderer for tree nodes in the tool window.
 */
public class ResultTreeRenderer extends JLabel
        implements TreeCellRenderer {

    private boolean selected;

    /**
     * Create a new cell renderer.
     */
    public ResultTreeRenderer() {
        super();
        setOpaque(false);
    }

    @Override
    public void paintComponent(final Graphics g) {
        g.setColor(getBackground());

        int offset = 0;
        if (getIcon() != null) {
            offset = getIcon().getIconWidth() + getIconTextGap();
        }

        g.fillRect(offset, 0, getWidth() - 1 - offset, getHeight() - 1);

        if (selected) {
            g.setColor(UIManager.getColor("Tree.selectionBorderColor"));
            g.drawRect(offset, 0, getWidth() - 1 - offset, getHeight() - 1);
        }

        super.paintComponent(g);
    }

    @Override
    public Component getTreeCellRendererComponent(final JTree tree,
                                                  final Object value,
                                                  final boolean selected,
                                                  final boolean expanded,
                                                  final boolean leaf,
                                                  final int row,
                                                  final boolean hasFocus) {
        this.selected = selected;

        final DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        if (node != null) {
            final Object userObject = node.getUserObject();
            if (userObject instanceof ResultTreeNode) {
                final ResultTreeNode treeNode
                        = (ResultTreeNode) userObject;

                if (expanded) {
                    setIcon(treeNode.getExpandedIcon());
                } else {
                    setIcon(treeNode.getCollapsedIcon());
                }

                setText(treeNode.toString());
                validate();

            } else {
                setIcon(null);
            }
        }

        setFont(tree.getFont());

        setForeground(UIManager.getColor(selected
                ? "Tree.selectionForeground" : "Tree.textForeground"));
        setBackground(UIManager.getColor(selected
                ? "Tree.selectionBackground" : "Tree.textBackground"));


        return this;
    }
}
