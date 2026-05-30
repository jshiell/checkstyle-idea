package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import org.infernus.idea.checkstyle.config.ConfigurationListener;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.ScanResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.InputStream;
import java.util.List;

import static org.infernus.idea.checkstyle.CheckStyleBundle.message;

/**
 * The tool window for CheckStyle scans.
 */
public class CheckStyleToolWindowPanel extends JPanel implements ConfigurationListener, DumbAware {

    public static final String ID_TOOLWINDOW = "CheckStyle";

    private static final Logger LOG = Logger.getInstance(CheckStyleToolWindowPanel.class);

    private static final String MAIN_ACTION_GROUP = "CheckStylePluginActions";
    private static final String TREE_ACTION_GROUP = "CheckStylePluginTreeActions";

    private final Project project;
    private final ToolWindow toolWindow;
    private final ConfigurationLocation defaultOverride;
    private final ComboBox<ConfigurationLocation> configurationOverrideCombo = new ComboBox<>();
    private final DefaultComboBoxModel<ConfigurationLocation> configurationOverrideModel = new DefaultComboBoxModel<>();

    private JTree resultsTree;
    private ResultTreeModel treeModel;

    // Extracted collaborators
    private ResultTreeNavigator treeNavigator;
    private ScanProgressManager progressManager;
    private ResultTreeBuilder treeBuilder;

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

        treeNavigator.expandTree(treeModel, 3);

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

        resultsTree.addTreeSelectionListener(new ToolWindowSelectionListener());
        resultsTree.addMouseListener(new ToolWindowMouseListener());
        resultsTree.addKeyListener(new ToolWindowKeyboardListener());
        resultsTree.setCellRenderer(new ResultTreeRenderer());

        // Build progress panel
        final JLabel progressLabel = new JLabel(" ");
        final JProgressBar progressBar = new JProgressBar(SwingConstants.HORIZONTAL);
        progressBar.setMinimum(0);
        final Dimension progressBarSize = new Dimension(100, progressBar.getPreferredSize().height);
        progressBar.setMinimumSize(progressBarSize);
        progressBar.setPreferredSize(progressBarSize);
        progressBar.setMaximumSize(progressBarSize);

        final JToolBar progressPanel = new JToolBar(SwingConstants.HORIZONTAL);
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

        // Wire up collaborators
        treeNavigator = new ResultTreeNavigator(project, resultsTree);
        progressManager = new ScanProgressManager(progressPanel, progressBar, progressLabel);
        treeBuilder = new ResultTreeBuilder(treeModel, progressManager, treeNavigator);

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
        progressManager.setProgressText(text);
    }

    /**
     * Increment the progress of the progress bar by a given number.
     *
     * @param size the size to increment by.
     */
    public void incrementProgressBarBy(final int size) {
        progressManager.incrementProgressBarBy(size);
    }

    /**
     * Should we scroll to the selected error in the editor automatically?
     *
     * @param scrollToSource true if the error should be scrolled to automatically.
     */
    public void setScrollToSource(final boolean scrollToSource) {
        final var updatedConfig = PluginConfigurationBuilder.from(configurationManager().getCurrent())
                .withScrollToSource(scrollToSource)
                .build();
        configurationManager().setCurrent(updatedConfig, false);
    }

    /**
     * Should we scroll to the selected error in the editor automatically?
     *
     * @return true if the error should be scrolled to automatically.
     */
    public boolean isScrollToSource() {
        return configurationManager().getCurrent().isScrollToSource();
    }

    public void jumpToSource() {
        treeNavigator.jumpToSource();
    }

    public void selectPreviousResult() {
        treeNavigator.selectPreviousResult();
    }

    public void selectNextResult() {
        treeNavigator.selectNextResult();
    }

    /**
     * Collapse the tree so that only the root node is visible.
     */
    public void collapseTree() {
        treeNavigator.collapseTree();
    }

    /**
     * Expand the error tree to the fullest.
     */
    public void expandTree() {
        treeNavigator.expandTree(treeModel, 3);
    }

    /**
     * Clear the results and display a 'scan in progress' notice.
     *
     * @param size the number of files being scanned.
     */
    public void displayInProgress(final int size) {
        treeBuilder.displayInProgress(size);
    }

    public void displayWarningResult(final String messageKey, final Object... messageArgs) {
        treeBuilder.displayWarningResult(messageKey, messageArgs);
    }

    /**
     * Clear the results and display notice to say an error occurred.
     *
     * @param error the error that occurred.
     */
    public void displayErrorResult(final Throwable error) {
        treeBuilder.displayErrorResult(error);
    }

    /**
     * Refresh the displayed results based on the current filter settings.
     */
    public void filterDisplayedResults() {
        treeBuilder.filterDisplayedResults();
    }

    /**
     * Display the passed results.
     *
     * @param scanResults    the results of the scan.
     * @param warningMessage a warning message to display about the results, if appropriate.
     */
    public void displayResults(final List<ScanResult> scanResults, final String warningMessage) {
        treeBuilder.displayResults(scanResults, warningMessage);
        invalidate();
        repaint();
    }

    public boolean isDisplayingErrors() {
        return treeBuilder.isDisplayingErrors();
    }

    public void setDisplayingErrors(final boolean displayingErrors) {
        treeBuilder.setDisplayingErrors(displayingErrors);
    }

    public boolean isDisplayingWarnings() {
        return treeBuilder.isDisplayingWarnings();
    }

    public void setDisplayingWarnings(final boolean displayingWarnings) {
        treeBuilder.setDisplayingWarnings(displayingWarnings);
    }

    public boolean isDisplayingInfo() {
        return treeBuilder.isDisplayingInfo();
    }

    public void setDisplayingInfo(final boolean displayingInfo) {
        treeBuilder.setDisplayingInfo(displayingInfo);
    }

    public void groupBy(final ResultGrouping grouping) {
        treeBuilder.groupBy(grouping);
    }

    public ResultGrouping groupedBy() {
        return treeBuilder.groupedBy();
    }

    private PluginConfigurationManager configurationManager() {
        return project.getService(PluginConfigurationManager.class);
    }

    /**
     * Listen for clicks and scroll to the error's source as necessary.
     */
    protected class ToolWindowMouseListener extends java.awt.event.MouseAdapter {

        @Override
        public void mouseClicked(final MouseEvent e) {
            if (e.getClickCount() < 2 && !isScrollToSource()) {
                return;
            }
            final TreePath treePath = resultsTree.getPathForLocation(e.getX(), e.getY());
            if (treePath != null) {
                treeNavigator.scrollToError(treePath, false);
            }
        }
    }

    /**
     * Listen for Enter key being pressed and scroll to the error's source.
     */
    protected class ToolWindowKeyboardListener extends java.awt.event.KeyAdapter {

        @Override
        public void keyPressed(final KeyEvent e) {
            if (e.getKeyCode() != KeyEvent.VK_ENTER) {
                return;
            }
            jumpToSource();
        }
    }

    /**
     * Listen for tree selection events and scroll to the error's source as necessary.
     */
    protected class ToolWindowSelectionListener implements TreeSelectionListener {

        @Override
        public void valueChanged(final TreeSelectionEvent e) {
            if (!isScrollToSource()) {
                return;
            }
            if (e.getPath() != null) {
                treeNavigator.scrollToError(e.getPath(), false);
            }
        }
    }

    enum Direction {
        FORWARD, BACKWARD
    }
}
