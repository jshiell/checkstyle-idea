package org.infernus.idea.checkstyle.toolwindow;

import java.io.Serial;
import java.util.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.checker.Problem;
import org.infernus.idea.checkstyle.csapi.SeverityLevel;
import org.jetbrains.annotations.Nullable;

import static java.util.Comparator.*;

public class ResultTreeModel extends DefaultTreeModel {

    @Serial
    private static final long serialVersionUID = 2161855162879365203L;

    private final ToggleableTreeNode visibleRootNode;

    public ResultTreeModel() {
        super(new DefaultMutableTreeNode());

        visibleRootNode = new ToggleableTreeNode();
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
        visibleRootNode.setUserObject(new ResultTreeNode(
                Objects.requireNonNullElseGet(messageText, () -> CheckStyleBundle.message("plugin.results.no-scan"))));

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
        for (final ToggleableTreeNode fileNode : visibleRootNode.getAllChildren()) {
            for (final ToggleableTreeNode problemNode : fileNode.getAllChildren()) {
                final ProblemResultTreeNode result = (ProblemResultTreeNode) problemNode.getUserObject();

                final boolean resultShouldBeVisible = contains(levels, result.getSeverity());
                if (problemNode.isVisible() != resultShouldBeVisible) {
                    problemNode.setVisible(resultShouldBeVisible);
                }
            }

            ((FileResultTreeNode) fileNode.getUserObject()).setVisibleProblems(fileNode.getChildCount());
            final boolean fileNodeShouldBeVisible = fileNode.getChildCount() > 0;
            if (fileNode.isVisible() != fileNodeShouldBeVisible) {
                fileNode.setVisible(fileNodeShouldBeVisible);
            }
        }

        if (sendEvents) {
            nodeStructureChanged(visibleRootNode);
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

        int itemCount = 0;
        for (final PsiFile file : sortedFileNames(results)) {
            final ToggleableTreeNode fileNode = new ToggleableTreeNode();
            final List<Problem> problems = results.get(file);

            int problemCount = 0;
            if (problems != null) {
                for (final Problem problem : problems) {
                    if (problem.severityLevel() != SeverityLevel.Ignore) {
                        final ResultTreeNode problemObj = new ProblemResultTreeNode(file, problem);

                        final ToggleableTreeNode problemNode = new ToggleableTreeNode(problemObj);
                        fileNode.add(problemNode);

                        ++problemCount;
                    }
                }
            }

            itemCount += problemCount;

            if (problemCount > 0) {
                final ResultTreeNode nodeObject = new FileResultTreeNode(file.getName(), problemCount);
                fileNode.setUserObject(nodeObject);

                visibleRootNode.add(fileNode);
            }
        }

        if (itemCount == 0) {
            setRootMessage("plugin.results.scan-no-results");
        } else {
            setRootText(CheckStyleBundle.message("plugin.results.scan-results", itemCount, results.size()));
        }

        filter(false, levels);
        nodeStructureChanged(visibleRootNode);
    }

    private Iterable<PsiFile> sortedFileNames(final Map<PsiFile, List<Problem>> results) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }
        final List<PsiFile> sortedFiles = new ArrayList<>(results.keySet());
        sortedFiles.sort(comparing(PsiFileSystemItem::getName));
        return sortedFiles;
    }
}
