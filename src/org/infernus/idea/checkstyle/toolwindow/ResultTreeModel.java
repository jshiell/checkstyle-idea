package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.psi.PsiFile;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import org.apache.commons.lang.ArrayUtils;
import org.infernus.idea.checkstyle.CheckStyleConstants;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.text.MessageFormat;
import java.util.*;

/**
 * Tree model for the scan results.
 */
public class ResultTreeModel extends DefaultTreeModel {

    private static final long serialVersionUID = 2161855162879365203L;

    private final DefaultMutableTreeNode visibleRootNode;

    /**
     * Create an empty result tree.
     */
    public ResultTreeModel() {
        super(new DefaultMutableTreeNode());

        visibleRootNode = new DefaultMutableTreeNode();
        ((DefaultMutableTreeNode) getRoot()).add(visibleRootNode);

        setRootMessage(null);
    }

    /**
     * Create a tree with the given model.
     *
     * @param results the model.
     */
    public ResultTreeModel(final Map<PsiFile, List<ProblemDescriptor>> results) {
        this();

        setModel(results);
    }

    /**
     * Clear the tree.
     */
    public void clear() {
        visibleRootNode.removeAllChildren();
        nodeStructureChanged(visibleRootNode);
    }

    public TreeNode getVisibleRoot() {
        return visibleRootNode;
    }

    /**
     * Set the root message.
     * <p/>
     * This will trigger a reload on the model, thanks to JTree's lack of support for
     * a node changed event for the root node.
     *
     * @param messageText the text to display.
     */
    public void setRootText(final String messageText) {
        if (messageText == null) {
            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);
            visibleRootNode.setUserObject(new ResultTreeNode(resources.getString("plugin.results.no-scan")));

        } else {
            visibleRootNode.setUserObject(new ResultTreeNode(messageText));
        }

        System.err.println("Visible node test is " + visibleRootNode.getUserObject());

        nodeChanged(visibleRootNode);
    }

    /**
     * Set the root message.
     * <p/>
     * This will trigger a reload on the model, thanks to JTree's lack of support for
     * a node changed event for the root node.
     *
     * @param messageKey the message key to display.
     */
    public void setRootMessage(final String messageKey) {
        if (messageKey == null) {
            setRootText(null);

        } else {
            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);

            setRootText(resources.getString(messageKey));
        }
    }

    /**
     * Display only the passed severity levels.
     *
     * @param levels the levels. Null is treated as 'none'.
     */
    public void filter(final SeverityLevel... levels) {
        filter(true, levels);
    }

    private void filter(final boolean sendEvents, final SeverityLevel... levels) {
        final Set<TogglableTreeNode> changedNodes = new HashSet<TogglableTreeNode>();

        for (int fileIndex = 0; fileIndex < visibleRootNode.getChildCount(); ++fileIndex) {
            final TogglableTreeNode fileNode = (TogglableTreeNode) visibleRootNode.getChildAt(fileIndex);

            for (final TogglableTreeNode problemNode : fileNode.getAllChildren()) {
                final ResultTreeNode result = (ResultTreeNode) problemNode.getUserObject();

                final boolean currentVisible = problemNode.isVisible();
                final boolean desiredVisible = levels != null && ArrayUtils.contains(levels, result.getSeverity());
                if (currentVisible != desiredVisible) {
                    problemNode.setVisible(desiredVisible);

                    changedNodes.add(fileNode);
                }
            }
        }

        if (sendEvents) {
            for (final TogglableTreeNode node : changedNodes) {
                nodeStructureChanged(node);
            }
        }
    }

    /**
     * Set the displayed model.
     *
     * @param results the model.
     */
    public void setModel(final Map<PsiFile, List<ProblemDescriptor>> results) {
        setModel(results, SeverityLevel.ERROR, SeverityLevel.WARNING, SeverityLevel.INFO);
    }

    /**
     * Set the displayed model.
     *
     * @param results the model.
     * @param levels  the levels to display.
     */
    public void setModel(final Map<PsiFile, List<ProblemDescriptor>> results,
                         final SeverityLevel... levels) {
        visibleRootNode.removeAllChildren();

        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);

        if (results == null || results.size() == 0) {
            setRootMessage("plugin.results.scan-no-results");

        } else {
            int itemCount = 0;
            for (final PsiFile file : results.keySet()) {
                final TogglableTreeNode fileNode = new TogglableTreeNode();
                final List<ProblemDescriptor> problems = results.get(file);
                if (problems != null) {
                    for (final ProblemDescriptor problem : problems) {
                        final ResultTreeNode problemObj = new ResultTreeNode(file, problem);

                        final TogglableTreeNode problemNode = new TogglableTreeNode(problemObj);
                        fileNode.add(problemNode);
                    }
                }

                int problemCount = 0;
                if (problems != null) {
                    problemCount = problems.size();
                }
                itemCount += problemCount;

                final ResultTreeNode nodeObject = new ResultTreeNode(file.getName(), problemCount);
                fileNode.setUserObject(nodeObject);

                visibleRootNode.add(fileNode);
            }

            final MessageFormat resultsMessage = new MessageFormat(
                    resources.getString("plugin.results.scan-results"));
            setRootText(resultsMessage.format(new Object[]{itemCount, results.size()}));
        }

        filter(false, levels);
        nodeStructureChanged(visibleRootNode);
    }
}
