package org.infernus.idea.checkstyle.toolwindow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.checker.Problem;
import org.infernus.idea.checkstyle.csapi.SeverityLevel;
import org.jetbrains.annotations.Nullable;

public class ResultTreeModel extends DefaultTreeModel {

    private static final long serialVersionUID = 2161855162879365203L;

    private final DefaultMutableTreeNode visibleRootNode;

    public ResultTreeModel() {
        super(new DefaultMutableTreeNode());

        visibleRootNode = new DefaultMutableTreeNode();
        ((DefaultMutableTreeNode) getRoot()).add(visibleRootNode);

        setRootMessage(null);
    }

    public void clear() {
        visibleRootNode.removeAllChildren();
        nodeStructureChanged(visibleRootNode);
    }

    public TreeNode getVisibleRoot() {
        return visibleRootNode;
    }

    /**
     * Set the root message.
     * <p>
     * This will trigger a reload on the model, thanks to JTree's lack of support for
     * a node changed event for the root node.
     *
     * @param messageText the text to display.
     */
    public void setRootText(@Nullable final String messageText) {
        if (messageText == null) {
            visibleRootNode.setUserObject(new ResultTreeNode(CheckStyleBundle.message("plugin.results.no-scan")));

        } else {
            visibleRootNode.setUserObject(new ResultTreeNode(messageText));
        }

        nodeChanged(visibleRootNode);
    }

    /**
     * Set the root message.
     * <p>
     * This will trigger a reload on the model, thanks to JTree's lack of support for
     * a node changed event for the root node.
     *
     * @param messageKey the message key to display.
     */
    public void setRootMessage(@Nullable final String messageKey,
                               @Nullable final Object... messageArgs) {
        if (messageKey == null) {
            setRootText(null);

        } else {
            setRootText(CheckStyleBundle.message(messageKey, messageArgs));
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
        final Set<TogglableTreeNode> changedNodes = new HashSet<>();

        for (int fileIndex = 0; fileIndex < visibleRootNode.getChildCount(); ++fileIndex) {
            final TogglableTreeNode fileNode = (TogglableTreeNode) visibleRootNode.getChildAt(fileIndex);

            for (final TogglableTreeNode problemNode : fileNode.getAllChildren()) {
                final ResultTreeNode result = (ResultTreeNode) problemNode.getUserObject();

                final boolean currentVisible = problemNode.isVisible();
                final boolean desiredVisible = levels != null && contains(levels, result.getSeverity());
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

    /*
     * This is a port from commons-lang 2.4, in order to get around the absence of commons-lang in
     * some packages of IDEA 7.x.
     */
    private boolean contains(final Object[] array, final Object objectToFind) {
        if (array == null) {
            return false;
        }
        if (objectToFind == null) {
            for (final Object anArray : array) {
                if (anArray == null) {
                    return true;
                }
            }
        } else {
            for (final Object anArray : array) {
                if (objectToFind.equals(anArray)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Set the displayed model.
     *
     * @param results the model.
     */
    public void setModel(final Map<PsiFile, List<Problem>> results) {
        setModel(results, SeverityLevel.Error, SeverityLevel.Warning, SeverityLevel.Info);
    }

    /**
     * Set the displayed model.
     *
     * @param results the model.
     * @param levels  the levels to display.
     */
    public void setModel(final Map<PsiFile, List<Problem>> results,
                         final SeverityLevel... levels) {
        visibleRootNode.removeAllChildren();

        if (results == null || results.size() == 0) {
            setRootMessage("plugin.results.scan-no-results");

        } else {
            int itemCount = 0;
            for (final PsiFile file : sortedFileNames(results)) {
                final TogglableTreeNode fileNode = new TogglableTreeNode();
                final List<Problem> problems = results.get(file);
                if (problems != null) {
                    for (final Problem problem : problems) {
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

            setRootText(CheckStyleBundle.message("plugin.results.scan-results", itemCount, results.size()));
        }

        filter(false, levels);
        nodeStructureChanged(visibleRootNode);
    }

    private Iterable<PsiFile> sortedFileNames(final Map<PsiFile, List<Problem>> results) {
        final List<PsiFile> sortedFiles = new ArrayList<>(results.keySet());
        Collections.sort(sortedFiles, (file1, file2) -> file1.getName().compareTo(file2.getName()));
        return sortedFiles;
    }
}
