package org.infernus.idea.checkstyle.toolwindow;

import java.io.Serial;
import java.util.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiJavaFile;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.checker.Problem;
import org.infernus.idea.checkstyle.csapi.SeverityLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Collections.emptyList;
import static java.util.Comparator.*;

public class ResultTreeModel extends DefaultTreeModel {

    @Serial
    private static final long serialVersionUID = 2161855162879365203L;
    public static final Set<SeverityLevel> DEFAULT_SEVERITIES = Set.of(SeverityLevel.Error, SeverityLevel.Warning, SeverityLevel.Info);

    private final ToggleableTreeNode visibleRootNode;

    private Set<SeverityLevel> displayedSeverities = DEFAULT_SEVERITIES;
    private ResultGrouping grouping = ResultGrouping.BY_FILE;
    private Map<PsiFile, List<Problem>> lastResults;

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

    private void rebuildTree() {
        visibleRootNode.removeAllChildren();

        switch (grouping) {
            case BY_FILE -> groupResultsByFile();
            case BY_PACKAGE -> groupResultsByPackage();
        }

        filterDisplayedTree();
        nodeStructureChanged(visibleRootNode);
    }

    /**
     * Display only the passed severity levels.
     *
     * @param severityLevels the levels. An empty set is treated as 'none'.
     */
    public void filter(@NotNull final Set<SeverityLevel> severityLevels) {
        this.displayedSeverities = severityLevels;

        filterDisplayedTree();
        nodeStructureChanged(visibleRootNode);
    }

    public void groupBy(@NotNull final ResultGrouping resultGrouping) {
        this.grouping = resultGrouping;

        rebuildTree();
    }

    public ResultGrouping groupedBy() {
        return grouping;
    }

    private void filterDisplayedTree() {
        filterNodeAndChildren(visibleRootNode);
    }

    private void filterNodeAndChildren(final ToggleableTreeNode node) {
        boolean nodeShouldBeVisible = true;

        for (final var childNode : node.getAllChildren()) {
            filterNodeAndChildren(childNode);
        }

        if (node.getUserObject() instanceof FileResultTreeInfo fileResultTreeInfo) {
            fileResultTreeInfo.setVisibleProblems(node.getChildCount());
            nodeShouldBeVisible = node.getChildCount() > 0;

        } else if (node.getUserObject() instanceof PackageTreeInfo packageTreeInfo) {
            packageTreeInfo.setVisibleProblems(node.getChildCount());
            nodeShouldBeVisible = node.getChildCount() > 0;

        } else if (node.getUserObject() instanceof ProblemResultTreeInfo problemResultTreeInfo) {
            nodeShouldBeVisible = displayedSeverities.contains(problemResultTreeInfo.getSeverity());
        }

        if (node.isVisible() != nodeShouldBeVisible) {
            node.setVisible(nodeShouldBeVisible);
        }
    }

    /**
     * Set the displayed model.
     *
     * @param results the model.
     */
    public void setModel(@NotNull  final Map<PsiFile, List<Problem>> results) {
        setModel(results, DEFAULT_SEVERITIES);
    }

    /**
     * Set the displayed model.
     *
     * @param results the model.
     * @param levels  the levels to display.
     */
    public void setModel(@NotNull final Map<PsiFile, List<Problem>> results,
                         @NotNull final Set<SeverityLevel> levels) {
        this.lastResults = results;
        this.displayedSeverities = levels;

        rebuildTree();
    }

    private void groupResultsByFile() {
        int problemCount = createFileNodes(sortByFileName(lastResults), visibleRootNode);
        setRootMessage(problemCount);
    }

    private List<PsiFile> sortByFileName(final Map<PsiFile, List<Problem>> results) {
        if (results == null || results.isEmpty()) {
            return emptyList();
        }
        var sortedFiles = new ArrayList<>(results.keySet());
        sortedFiles.sort(comparing(PsiFileSystemItem::getName));
        return sortedFiles;
    }

    private void groupResultsByPackage() {
        int problemCount = 0;

        var groupedByPackage = groupByPackageName(lastResults);
        for (String packageName : groupedByPackage.keySet()) {
            final var packageNode = new ToggleableTreeNode();

            var childProblemCount = createFileNodes(groupedByPackage.getOrDefault(packageName, emptyList()), packageNode);
            if (childProblemCount > 0) {
                final var packageInfo = new PackageTreeInfo(packageName, childProblemCount);
                packageNode.setUserObject(packageInfo);
                visibleRootNode.add(packageNode);
            }

            problemCount += childProblemCount;
        }

        setRootMessage(problemCount);
    }

    private int createFileNodes(final List<PsiFile> files,
                                final ToggleableTreeNode parentNode) {
        int problemCount = 0;
        for (final PsiFile file : files) {
            final var fileNode = new ToggleableTreeNode();
            final var problems = lastResults.getOrDefault(file, emptyList());

            int childProblemCount = 0;
            for (final Problem problem : problems) {
                if (problem.severityLevel() != SeverityLevel.Ignore) {
                    final var problemInfo = new ProblemResultTreeInfo(file, problem);
                    fileNode.add(new ToggleableTreeNode(problemInfo));

                    ++childProblemCount;
                }
            }

            if (childProblemCount > 0) {
                var nodeObject = new FileResultTreeInfo(file.getName(), childProblemCount);
                fileNode.setUserObject(nodeObject);

                parentNode.add(fileNode);
            }

            problemCount += childProblemCount;
        }
        return problemCount;
    }

    private SortedMap<String, List<PsiFile>> groupByPackageName(final Map<PsiFile, List<Problem>> results) {
        if (results == null || results.isEmpty()) {
            return Collections.emptySortedMap();
        }
        var groupedByPackage = new TreeMap<String, List<PsiFile>>();
        for (var result : results.keySet()) {
            var filePackage = CheckStyleBundle.message("plugin.results.unknown-package");
            if (result instanceof PsiJavaFile javaFile) {
                filePackage = javaFile.getPackageName();

                if (filePackage.trim().isEmpty()) {
                    filePackage = CheckStyleBundle.message("plugin.results.root-package");
                }
            }
            groupedByPackage.computeIfAbsent(filePackage,  key -> new ArrayList<>()).add(result);
        }
        return groupedByPackage;
    }

    private void setRootMessage(final int problemCount) {
        if (problemCount == 0) {
            setRootMessage("plugin.results.scan-no-results");
        } else {
            setRootText(CheckStyleBundle.message("plugin.results.scan-results", problemCount, lastResults.size()));
        }
    }
}
