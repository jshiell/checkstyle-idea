package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.Function;
import com.intellij.util.ui.JBUI;
import org.infernus.idea.checkstyle.config.ConfigurationListener;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.csapi.SeverityLevel;
import org.infernus.idea.checkstyle.exception.CheckStylePluginParseException;
import org.infernus.idea.checkstyle.exception.CheckstyleToolException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.ScanResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.infernus.idea.checkstyle.CheckStyleBundle.message;
import static org.infernus.idea.checkstyle.util.Strings.isBlank;

/**
 * The tool window for CheckStyle scans.
 */
public class CheckStyleToolWindowPanel extends JPanel implements ConfigurationListener, DumbAware {

    public static final String ID_TOOLWINDOW = "CheckStyle";

    /**
     * Logger for this class.
     */
    private static final Logger LOG = Logger.getInstance(CheckStyleToolWindowPanel.class);

    private static final String MAIN_ACTION_GROUP = "CheckStylePluginActions";
    private static final String TREE_ACTION_GROUP = "CheckStylePluginTreeActions";

    private static final Map<Pattern, String> CHECKSTYLE_ERROR_PATTERNS = new HashMap<>();

    private final Project project;
    private final ToolWindow toolWindow;
    private final ConfigurationLocation defaultOverride;
    private final ComboBox<ConfigurationLocation> configurationOverrideCombo = new ComboBox<>();
    private final DefaultComboBoxModel<ConfigurationLocation> configurationOverrideModel = new DefaultComboBoxModel<>();

    private boolean displayingErrors = true;
    private boolean displayingWarnings = true;
    private boolean displayingInfo = true;

    private JTree resultsTree;
    private JToolBar progressPanel;
    private JProgressBar progressBar;
    private JLabel progressLabel;
    private ResultTreeModel treeModel;
    private boolean scrollToSource;

    static {
        try {
            CHECKSTYLE_ERROR_PATTERNS.put(
                    Pattern.compile("Property \\$\\{([^}]*)} has not been set"),
                    "plugin.results.error.missing-property");
            CHECKSTYLE_ERROR_PATTERNS.put(
                    Pattern.compile("Unable to instantiate (.*)"),
                    "plugin.results.error.instantiation-failed");

        } catch (Throwable t) {
            LOG.warn("Pattern mappings could not be instantiated.", t);
        }
    }

    /**
     * Create a tool window for the given project.
     *
     * @param project the project.
     */
    public CheckStyleToolWindowPanel(final ToolWindow toolWindow, final Project project) {
        super(new BorderLayout());

        this.toolWindow = toolWindow;
        this.project = project;

        defaultOverride = createDefaultOverride();

        configurationChanged();
        configurationManager().addConfigurationListener(this);

        final ActionGroup mainActionGroup = (ActionGroup)
                ActionManager.getInstance().getAction(MAIN_ACTION_GROUP);
        final ActionToolbar mainToolbar = ActionManager.getInstance().createActionToolbar(
                ID_TOOLWINDOW, mainActionGroup, false);
        mainToolbar.setTargetComponent(this);

        final ActionGroup treeActionGroup = (ActionGroup)
                ActionManager.getInstance().getAction(TREE_ACTION_GROUP);
        final ActionToolbar treeToolbar = ActionManager.getInstance().createActionToolbar(
                ID_TOOLWINDOW, treeActionGroup, false);
        treeToolbar.setTargetComponent(this);

        final Box toolBarBox = Box.createHorizontalBox();
        toolBarBox.add(mainToolbar.getComponent());
        toolBarBox.add(treeToolbar.getComponent());

        setBorder(JBUI.Borders.empty(1));
        add(toolBarBox, BorderLayout.WEST);
        add(createToolPanel(), BorderLayout.CENTER);

        expandTree();

        mainToolbar.getComponent().setVisible(true);
    }

    private @NotNull ConfigurationLocation createDefaultOverride() {
        final ConfigurationLocation overrideLocation = new ConfigurationLocation("default", ConfigurationType.LOCAL_FILE, project) {
            @Override
            protected @NotNull InputStream resolveFile(@NotNull final ClassLoader checkstyleClassLoader) {
                throw new UnsupportedOperationException("Default override should never be resolved to a file.");
            }

            @Override
            public Object clone() {
                return this;
            }
        };
        overrideLocation.setDescription(message("plugin.toolwindow.default-file"));
        return overrideLocation;
    }

    public ConfigurationLocation getSelectedOverride() {
        final Object selectedItem = configurationOverrideModel.getSelectedItem();
        if (defaultOverride.equals(selectedItem)) {
            return null;
        }
        return (ConfigurationLocation) selectedItem;
    }

    private JPanel createToolPanel() {
        configurationOverrideCombo.setModel(configurationOverrideModel);
        final int preferredHeight = configurationOverrideCombo.getPreferredSize().height;
        configurationOverrideCombo.setPreferredSize(new Dimension(250, preferredHeight));
        configurationOverrideCombo.setMaximumSize(new Dimension(350, preferredHeight));

        treeModel = new ResultTreeModel();

        resultsTree = new Tree(treeModel);
        resultsTree.setRootVisible(false);

        final TreeSelectionListener treeSelectionListener = new ToolWindowSelectionListener();
        resultsTree.addTreeSelectionListener(treeSelectionListener);
        final MouseListener treeMouseListener = new ToolWindowMouseListener();
        resultsTree.addMouseListener(treeMouseListener);
        resultsTree.addKeyListener(new ToolWindowKeyboardListener());
        resultsTree.setCellRenderer(new ResultTreeRenderer());

        progressLabel = new JLabel(" ");
        progressBar = new JProgressBar(SwingConstants.HORIZONTAL);
        progressBar.setMinimum(0);
        final Dimension progressBarSize = new Dimension(100, progressBar.getPreferredSize().height);
        progressBar.setMinimumSize(progressBarSize);
        progressBar.setPreferredSize(progressBarSize);
        progressBar.setMaximumSize(progressBarSize);

        progressPanel = new JToolBar(SwingConstants.HORIZONTAL);
        progressPanel.add(Box.createHorizontalStrut(4));
        progressPanel.add(new JLabel(message("plugin.toolwindow.override")));
        progressPanel.add(Box.createHorizontalStrut(4));
        progressPanel.add(configurationOverrideCombo);
        progressPanel.add(Box.createHorizontalStrut(4));
        progressPanel.addSeparator();
        progressPanel.add(Box.createHorizontalStrut(4));
        progressPanel.add(progressLabel);
        progressPanel.add(Box.createHorizontalGlue());
        progressPanel.setFloatable(false);
        progressPanel.setOpaque(false);
        progressPanel.setBorder(null);

        final JPanel toolPanel = new JPanel(new BorderLayout());
        toolPanel.add(new JBScrollPane(resultsTree), BorderLayout.CENTER);
        toolPanel.add(progressPanel, BorderLayout.NORTH);

        ToolTipManager.sharedInstance().registerComponent(resultsTree);

        return toolPanel;
    }

    @Nullable
    public static CheckStyleToolWindowPanel panelFor(final Project project) {
        final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);

        final ToolWindow toolWindow = toolWindowManager.getToolWindow(ID_TOOLWINDOW);
        if (toolWindow == null) {
            LOG.debug("Couldn't get tool window for ID " + ID_TOOLWINDOW);
            return null;
        }

        for (Content currentContent : toolWindow.getContentManager().getContents()) {
            if (currentContent.getComponent() instanceof CheckStyleToolWindowPanel) {
                return (CheckStyleToolWindowPanel) currentContent.getComponent();
            }
        }

        LOG.debug("Could not find tool window panel on tool window with ID " + ID_TOOLWINDOW);
        return null;
    }

    @Override
    public void configurationChanged() {
        configurationOverrideModel.removeAllElements();

        configurationOverrideModel.addElement(defaultOverride);
        configurationManager().getCurrent().getLocations().forEach(configurationOverrideModel::addElement);
        configurationOverrideModel.setSelectedItem(defaultOverride);
    }

    public void showToolWindow() {
        toolWindow.show(null);
    }

    /**
     * Update the progress text.
     *
     * @param text the new progress text, or null to clear.
     */
    public void setProgressText(@Nullable final String text) {
        if (isBlank(text)) {
            progressLabel.setText(" ");
        } else {
            progressLabel.setText(text);
        }
        progressLabel.validate();
    }

    /**
     * Show and reset the progress bar.
     */
    private void resetProgressBar() {
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
    private void setProgressBarMax(final int max) {
        progressBar.setMaximum(max);
        resetProgressBar();
    }

    /**
     * Increment the progress of the progress bar by a given number.
     * <p>
     * You should call {@link #displayInProgress(int)} first for useful semantics.
     *
     * @param size the size to increment by.
     */
    public void incrementProgressBarBy(final int size) {
        if (progressBar.getValue() < progressBar.getMaximum()) {
            progressBar.setValue(progressBar.getValue() + size);
        }
    }

    private void clearProgress() {
        final int progressIndex = progressPanel.getComponentIndex(progressBar);
        if (progressIndex != -1) {
            progressPanel.remove(progressIndex);
            progressPanel.revalidate();
            progressPanel.repaint();
        }
        setProgressText(null);
    }

    /**
     * Scroll to the error specified by the given tree path, or do nothing
     * if no error is specified.
     *
     * @param treePath the tree path to scroll to.
     */
    private void scrollToError(final TreePath treePath) {
        final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) treePath.getLastPathComponent();
        if (treeNode == null || !(treeNode.getUserObject() instanceof ProblemResultTreeInfo nodeInfo)) {
            return;
        }

        if (nodeInfo.getFile() == null || nodeInfo.getProblem() == null) {
            return; // no problem here :-)
        }

        final VirtualFile virtualFile = nodeInfo.getFile().getVirtualFile();
        if (virtualFile == null || !virtualFile.exists()) {
            return;
        }

        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        ApplicationManager.getApplication().invokeLater(() -> {
            final FileEditor[] editor = fileEditorManager.openFile(
                    virtualFile, true);

            if (editor.length > 0 && editor[0] instanceof TextEditor) {
                final LogicalPosition problemPos = new LogicalPosition(
                        Math.max(lineFor(nodeInfo) - 1, 0), Math.max(columnFor(nodeInfo), 0));

                final Editor textEditor = ((TextEditor) editor[0]).getEditor();
                textEditor.getCaretModel().moveToLogicalPosition(problemPos);
                textEditor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
            }
        }, ModalityState.nonModal());
    }

    private int lineFor(final ProblemResultTreeInfo nodeInfo) {
        return nodeInfo.getProblem().line();
    }

    private int columnFor(final ProblemResultTreeInfo nodeInfo) {
        return nodeInfo.getProblem().column();
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

    public void selectPreviousResult() {
        final TreePath previousResultPath = findPreviousLeaf(resultsTree, resultsTree.getSelectionPath());
        if (previousResultPath != null) {
            resultsTree.setSelectionPath(previousResultPath);
            resultsTree.scrollPathToVisible(previousResultPath);
        }
    }

    public void selectNextResult() {
        final TreePath nextResultPath = findNextLeaf(resultsTree, resultsTree.getSelectionPath());
        if (nextResultPath != null) {
            resultsTree.setSelectionPath(nextResultPath);
            resultsTree.scrollPathToVisible(nextResultPath);
        }
    }

    private TreePath findPreviousLeaf(@NotNull final JTree tree,
                                       @Nullable final TreePath startingPath) {
        // TODO: edge case here when current path is not a leaf, we still go forward

        return findSubsequentLeaf(tree, startingPath, i -> i - 1);
    }

    private TreePath findNextLeaf(@NotNull final JTree tree,
                                       @Nullable final TreePath startingPath) {
        return findSubsequentLeaf(tree, startingPath, i -> i + 1);
    }

    private TreePath findSubsequentLeaf(@NotNull final JTree tree,
                                       @Nullable final TreePath startingPath,
                                       @NotNull final Function<Integer, Integer> nextIndexFunction) {
        if (startingPath == null) {
            return null;
        }

        final TreeModel model = tree.getModel();
        final Object node = startingPath.getLastPathComponent();

        if (!model.isLeaf(node)) {
            TreePath next = startingPath.pathByAddingChild(model.getChild(node, 0));
            while (!model.isLeaf(next.getLastPathComponent())) {
                next = next.pathByAddingChild(model.getChild(next.getLastPathComponent(), 0));
            }
            return next;
        }

        TreePath currentPath = startingPath;
        TreePath parentPath = currentPath.getParentPath();
        while (parentPath != null) {
            final Object parent = parentPath.getLastPathComponent();
            final int childIndex = model.getIndexOfChild(parent, currentPath.getLastPathComponent());
            final int nextChildIndex = nextIndexFunction.apply(childIndex);
            if (nextChildIndex >= 0 && nextChildIndex < model.getChildCount(parent)) {
                final Object nextSibling = model.getChild(parent, nextChildIndex);
                TreePath nextSiblingPath = parentPath.pathByAddingChild(nextSibling);
                while (!model.isLeaf(nextSiblingPath.getLastPathComponent())) {
                    nextSiblingPath = nextSiblingPath.pathByAddingChild(model.getChild(nextSiblingPath.getLastPathComponent(), 0));
                }
                return nextSiblingPath;
            }
            currentPath = parentPath;
            parentPath = currentPath.getParentPath();
        }

        return null;
    }

    /**
     * Listen for clicks and scroll to the error's source as necessary.
     */
    protected class ToolWindowMouseListener extends MouseAdapter {

        @Override
        public void mouseClicked(final MouseEvent e) {
            if (!scrollToSource && e.getClickCount() < 2) {
                return;
            }

            final TreePath treePath = resultsTree.getPathForLocation(e.getX(), e.getY());
            if (treePath != null) {
                scrollToError(treePath);
            }
        }
    }

    /**
     * Listen for Enter key being pressed and scroll to the error's source
     */
    protected class ToolWindowKeyboardListener extends KeyAdapter {

        @Override
        public void keyPressed(final KeyEvent e) {
            if (e.getKeyCode() != KeyEvent.VK_ENTER) {
                return;
            }

            final TreePath treePath = resultsTree.getSelectionPath();
            if (treePath != null) {
                scrollToError(treePath);
            }
        }
    }

    /**
     * Listen for tree selection events and scroll to the error's source as necessary.
     */
    protected class ToolWindowSelectionListener implements TreeSelectionListener {

        @Override
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
    private void expandTree(final int level) {
        expandNode(resultsTree, treeModel.getVisibleRoot(),
                new TreePath(treeModel.getPathToRoot(treeModel.getVisibleRoot())), level);
    }

    /**
     * Clear the results and display a 'scan in progress' notice.
     *
     * @param size the number of files being scanned.
     */
    public void displayInProgress(final int size) {
        setProgressBarMax(size);

        treeModel.clear();
        treeModel.setRootMessage("plugin.results.in-progress");
    }

    public void displayWarningResult(final String messageKey,
                                     final Object... messageArgs) {
        clearProgress();

        treeModel.clear();

        treeModel.setRootMessage(messageKey, messageArgs);
    }

    /**
     * Clear the results and display notice to say an error occurred.
     *
     * @param error the error that occurred.
     */
    public void displayErrorResult(final Throwable error) {
        // match some friendly error messages.
        String errorText = null;
        if (error instanceof CheckstyleToolException && error.getCause() != null) {
            for (final Map.Entry<Pattern, String> errorPatternEntry
                    : CHECKSTYLE_ERROR_PATTERNS.entrySet()) {
                final Matcher errorMatcher
                        = errorPatternEntry.getKey().matcher(error.getCause().getMessage());
                if (errorMatcher.find()) {
                    final Object[] args = new Object[errorMatcher.groupCount()];

                    for (int i = 0; i < errorMatcher.groupCount(); ++i) {
                        args[i] = errorMatcher.group(i + 1);
                    }

                    errorText = message(errorPatternEntry.getValue(), args);
                }
            }
        }

        if (errorText == null) {
            if (error instanceof CheckStylePluginParseException) {
                errorText = message("plugin.results.unparseable");
            } else {
                errorText = message("plugin.results.error");
            }
        }

        treeModel.clear();
        treeModel.setRootText(errorText);

        clearProgress();
    }

    private Set<SeverityLevel> getDisplayedSeverities() {
        final var severityLevels = new HashSet<SeverityLevel>();

        if (displayingErrors) {
            severityLevels.add(SeverityLevel.Error);
        }

        if (displayingWarnings) {
            severityLevels.add(SeverityLevel.Warning);
        }

        if (displayingInfo) {
            severityLevels.add(SeverityLevel.Info);
        }

        return severityLevels;
    }

    /**
     * Refresh the displayed results based on the current filter settings.
     */
    public void filterDisplayedResults() {
        treeModel.filter(getDisplayedSeverities());
        expandTree();
    }

    /**
     * Display the passed results.
     *
     * @param scanResults the results of the scan.
     * @param warningMessage a warning message to display about the results, if appropriate.
     */
    public void displayResults(final List<ScanResult> scanResults,
                               final String warningMessage) {
        treeModel.setModel(scanResults, getDisplayedSeverities());

        invalidate();
        repaint();

        expandTree();
        clearProgress();
        if (warningMessage != null) {
            setProgressText(warningMessage);
        }
    }

    public boolean isDisplayingErrors() {
        return displayingErrors;
    }

    public void setDisplayingErrors(final boolean displayingErrors) {
        this.displayingErrors = displayingErrors;
    }

    public boolean isDisplayingWarnings() {
        return displayingWarnings;
    }

    public void setDisplayingWarnings(final boolean displayingWarnings) {
        this.displayingWarnings = displayingWarnings;
    }

    public boolean isDisplayingInfo() {
        return displayingInfo;
    }

    public void setDisplayingInfo(final boolean displayingInfo) {
        this.displayingInfo = displayingInfo;
    }

    public void groupBy(final ResultGrouping grouping) {
        treeModel.groupBy(grouping);
    }

    public ResultGrouping groupedBy() {
        return treeModel.groupedBy();
    }

    private PluginConfigurationManager configurationManager() {
        return project.getService(PluginConfigurationManager.class);
    }
}
