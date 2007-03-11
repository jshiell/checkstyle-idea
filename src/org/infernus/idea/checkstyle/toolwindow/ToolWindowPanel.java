package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import org.infernus.idea.checkstyle.CheckStyleConstants;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.util.ExtendedProblemDescriptor;
import org.infernus.idea.checkstyle.util.IDEAUtilities;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * The tool window for CheckStyle scans.
 *
 * @author James Shiell
 * @version 1.0
 */
public class ToolWindowPanel extends JPanel {

    private final MouseListener treeMouseListener = new ToolWindowMouseListener();
    private final TreeSelectionListener treeSelectionListener
            = new ToolWindowSelectionListener();

    private final Project project;
    private final JTree resultsTree;
    private final JToolBar progressPanel;
    private final JProgressBar progressBar;
    private final JLabel progressLabel;
    private final DefaultMutableTreeNode visibleRootNode;

    private DefaultTreeModel treeModel;
    private boolean scrollToSource;

    /**
     * Create a tool window for the given project.
     *
     * @param project the project.
     */
    public ToolWindowPanel(final Project project)

    {
        super(new BorderLayout());

        this.project = project;

        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);

        setBorder(new EmptyBorder(1, 1, 1, 1));

        final CheckStylePlugin checkStylePlugin
                = project.getComponent(CheckStylePlugin.class);
        if (checkStylePlugin == null) {
            throw new IllegalStateException("Couldn't get checkstyle plugin");
        }

        // Create the toolbar
        final ActionGroup actionGroup = (ActionGroup)
                ActionManager.getInstance().getAction(CheckStyleConstants.ACTION_GROUP);
        final ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(
                checkStylePlugin.getToolWindowId(), actionGroup, false);
        add(toolbar.getComponent(), BorderLayout.WEST);

        // Create the tree
        visibleRootNode = new DefaultMutableTreeNode(new ToolWindowTreeNode(
                resources.getString("plugin.results.no-scan")));
        treeModel = new DefaultTreeModel(visibleRootNode);

        resultsTree = new JTree(treeModel);
        resultsTree.addTreeSelectionListener(treeSelectionListener);
        resultsTree.addMouseListener(treeMouseListener);
        resultsTree.setCellRenderer(new ToolWindowCellRenderer());

        progressLabel = new JLabel(" ");
        progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
        progressBar.setMinimum(0);
        final Dimension progressBarSize = new Dimension(
                100, progressBar.getPreferredSize().height);
        progressBar.setMinimumSize(progressBarSize);
        progressBar.setPreferredSize(progressBarSize);
        progressBar.setMaximumSize(progressBarSize);

        progressPanel = new JToolBar(JToolBar.HORIZONTAL);
        progressPanel.add(progressLabel);
        progressPanel.add(Box.createHorizontalGlue());
        progressPanel.setFloatable(false);
        progressPanel.setBackground(UIManager.getColor("Panel.background"));
        progressPanel.setBorder(null);

        final JPanel toolPanel = new JPanel(new BorderLayout());

        toolPanel.add(new JScrollPane(resultsTree), BorderLayout.CENTER);
        toolPanel.add(progressPanel, BorderLayout.NORTH);

        add(toolPanel, BorderLayout.CENTER);

        expandTree();

        ToolTipManager.sharedInstance().registerComponent(resultsTree);
        toolbar.getComponent().setVisible(true);
    }

    /**
     * Update the progress text.
     *
     * @param text the new progress text, or null to clear.
     */
    public void setProgressText(final String text) {
        if (text == null || text.trim().length() == 0) {
            progressLabel.setText(" ");
        } else {
            progressLabel.setText(text);
        }
        progressLabel.validate();
    }

    /**
     * Show and reset the progress bar.
     */
    public void resetProgressBar() {
        progressBar.setValue(0);

        // show if necessary
        if (progressPanel.getComponentIndex(progressBar) == -1) {
            progressPanel.add(progressBar);
        }

        progressPanel.revalidate();
    }

    /**
     * Set the maxium limit, then show and reset the progress bar.
     *
     * @param max the maximum limit of the progress bar.
     */
    public void setProgressBarMax(final int max) {
        progressBar.setMaximum(max);

        resetProgressBar();
    }

    /**
     * Increment the progress of the progress bar.
     * <p/>
     * You should call {@link #setProgressBarMax(int)} first for useful semantics.
     */
    public void incrementProgressBar() {
        if (progressBar.getValue() < progressBar.getMaximum()) {
            progressBar.setValue(progressBar.getValue() + 1);
        }
    }

    /**
     * Hides the progress bar.
     */
    public void clearProgressBar() {
        final int progressIndex = progressPanel.getComponentIndex(progressBar);
        if (progressIndex != -1) {
            progressPanel.remove(progressIndex);
            progressPanel.revalidate();
            progressPanel.repaint();
        }
    }

    /**
     * Scroll to the error specified by the given tree path, or do nothing
     * if no error is specified.
     *
     * @param treePath the tree path to scroll to.
     */
    private void scrollToError(final TreePath treePath) {
        final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) treePath.getLastPathComponent();
        if (treeNode == null) {
            return;
        }

        final ToolWindowTreeNode nodeInfo = (ToolWindowTreeNode) treeNode.getUserObject();
        if (nodeInfo.getFile() == null || nodeInfo.getProblem() == null) {
            return; // no problem here :-)
        }

        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        final FileEditor[] editor = fileEditorManager.openFile(
                nodeInfo.getFile().getVirtualFile(), true);

        if (editor != null && editor.length > 0 && editor[0] instanceof TextEditor) {
            final int column = nodeInfo.getProblem() instanceof ExtendedProblemDescriptor
                ? ((ExtendedProblemDescriptor) nodeInfo.getProblem()).getColumn() : 0;
            final int line = nodeInfo.getProblem() instanceof ExtendedProblemDescriptor
                ? ((ExtendedProblemDescriptor) nodeInfo.getProblem()).getLine()
                    : nodeInfo.getProblem().getLineNumber();
            final LogicalPosition problemPos = new LogicalPosition(
                    line - 1, column);

            ((TextEditor) editor[0]).getEditor().getCaretModel().moveToLogicalPosition(problemPos);
            ((TextEditor) editor[0]).getEditor().getScrollingModel().scrollToCaret(ScrollType.CENTER);
        }
    }

    /**
     * Should we scroll to the selected error in the editor automatically?
     *
     * @param scrollToSource true if the error should be scrolled to automatically.
     */
    public void setScrollToSource(final boolean scrollToSource) {
        this.scrollToSource = scrollToSource;
    }

    /**
     * Should we scroll to the selected error in the editor automatically?
     *
     * @return true if the error should be scrolled to automatically.
     */
    public boolean isScrollToSource() {
        return scrollToSource;
    }


    /**
     * Listen for clicks and scroll to the error's source as necessary.
     */
    protected class ToolWindowMouseListener extends MouseAdapter {

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseClicked(final MouseEvent e) {
            if (!scrollToSource && e.getClickCount() < 2) {
                return;
            }

            final TreePath treePath = resultsTree.getPathForLocation(
                    e.getX(), e.getY());

            if (treePath != null) {
                scrollToError(treePath);
            }
        }

    }

    /**
     * Listen for tree selection events and scroll to the error's source as necessary.
     */
    protected class ToolWindowSelectionListener implements TreeSelectionListener {

        /**
         * {@inheritDoc}
         */
        public void valueChanged(final TreeSelectionEvent e) {
            if (!scrollToSource) {
                return;
            }

            if (e.getPath() != null) {
                scrollToError(e.getPath());
            }
        }

    }

    /**
     * Collapse the tree so that only the root node is visible.
     */
    public void collapseTree() {
        for (int i = 1; i < resultsTree.getRowCount(); ++i) {
            resultsTree.collapseRow(i);
        }
    }

    /**
     * Expand the error tree to the fullest.
     */
    public void expandTree() {
        expandTree(3);
    }

    /**
     * Expand the given tree to the given level, starting from the given node
     * and path.
     *
     * @param tree  The tree to be expanded
     * @param node  The node to start from
     * @param path  The path to start from
     * @param level The number of levels to expand to
     */
    private static void expandNode(final JTree tree,
                                   final TreeNode node,
                                   final TreePath path,
                                   final int level) {
        if (level <= 0) {
            return;
        }

        tree.expandPath(path);

        for (int i = 0; i < node.getChildCount(); ++i) {
            final TreeNode childNode = node.getChildAt(i);
            expandNode(tree, childNode, path.pathByAddingChild(childNode), level - 1);
        }
    }

    /**
     * Expand the error tree to the given level.
     *
     * @param level The level to expand to
     */
    private void expandTree(int level) {
        expandNode(resultsTree, (TreeNode) resultsTree.getModel().getRoot(),
                new TreePath(visibleRootNode), level);
    }

    /**
     * Clear the results and display a 'scan in progress' notice.
     */
    public void displayInProgress() {
        visibleRootNode.removeAllChildren();

        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);

        ((ToolWindowTreeNode) visibleRootNode.getUserObject()).setText(
                resources.getString("plugin.results.in-progress"));

        treeModel.reload();
    }

    /**
     * Clear the results and display notice to say an error occurred.
     */
    public void displayErrorResult() {
        visibleRootNode.removeAllChildren();

        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);

        ((ToolWindowTreeNode) visibleRootNode.getUserObject()).setText(
                resources.getString("plugin.results.error"));

        treeModel.reload();
    }

    /**
     * Display the passed results.
     *
     * @param results the map of checked files to problem descriptors.
     */
    public void displayResults(final Map<PsiFile, List<ProblemDescriptor>> results) {
        visibleRootNode.removeAllChildren();

        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);

        if (results == null || results.size() == 0) {
            ((ToolWindowTreeNode) visibleRootNode.getUserObject()).setText(
                    resources.getString("plugin.results.scan-no-results"));

        } else {
            final MessageFormat fileResultMessage = new MessageFormat(
                    resources.getString("plugin.results.scan-file-result"));

            int itemCount = 0;
            for (final PsiFile file : results.keySet()) {
                final DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode();
                final List<ProblemDescriptor> problems = results.get(file);
                if (problems != null) {
                    for (final ProblemDescriptor problem : problems) {
                        final ToolWindowTreeNode problemObj = new ToolWindowTreeNode(file, problem);

                        final SeverityLevel severity = problem instanceof ExtendedProblemDescriptor
                                ? ((ExtendedProblemDescriptor) problem).getSeverity() : null;
                        final Icon problemIcon;
                        if (severity != null && SeverityLevel.IGNORE.equals(severity)) {
                            problemIcon = IDEAUtilities.getIcon("/compiler/hideWarnings.png");
                        } else if (severity != null && SeverityLevel.WARNING.equals(severity)) {
                            problemIcon = IDEAUtilities.getIcon("/compiler/warning.png");
                        } else if (severity != null && SeverityLevel.INFO.equals(severity)) {
                            problemIcon = IDEAUtilities.getIcon("/compiler/information.png");
                        } else {
                            problemIcon = IDEAUtilities.getIcon("/compiler/error.png");
                        }

                        problemObj.setExpandedIcon(problemIcon);
                        problemObj.setCollapsedIcon(problemIcon);

                        final DefaultMutableTreeNode problemNode = new DefaultMutableTreeNode(
                                problemObj);
                        fileNode.add(problemNode);
                    }
                }

                final int problemCount = problems != null ? problems.size() : 0;
                itemCount += problemCount;

                final ToolWindowTreeNode nodeObject = new ToolWindowTreeNode(
                        fileResultMessage.format(new Object[]{file.getName(), problemCount}));

                final Icon fileIcon = IDEAUtilities.getIcon("/fileTypes/java.png");
                nodeObject.setExpandedIcon(fileIcon);
                nodeObject.setCollapsedIcon(fileIcon);

                fileNode.setUserObject(nodeObject);

                visibleRootNode.add(fileNode);
            }

            final MessageFormat resultsMessage = new MessageFormat(
                    resources.getString("plugin.results.scan-results"));
            ((ToolWindowTreeNode) visibleRootNode.getUserObject()).setText(
                    resultsMessage.format(new Object[]{itemCount, results.size()}));
        }

        treeModel.reload();
    }

}
