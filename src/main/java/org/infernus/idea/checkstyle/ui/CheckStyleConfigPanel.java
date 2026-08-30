package org.infernus.idea.checkstyle.ui;

import com.intellij.CommonBundle;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.CheckstyleArtifactDownloader;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.ThirdPartyJarCache;
import org.infernus.idea.checkstyle.VersionListReader;
import org.infernus.idea.checkstyle.actions.DetectConventionalConfigurationLocation;
import org.infernus.idea.checkstyle.checker.CheckerFactoryCache;
import org.infernus.idea.checkstyle.config.ConfigurationExporter;
import org.infernus.idea.checkstyle.config.ConventionalConfigurationLocationScanner;
import org.infernus.idea.checkstyle.config.ConventionalConfigurationLocationScanner.ScanOutcome;
import org.infernus.idea.checkstyle.config.ConventionalConfigurationLocationScanner.ScanResult;
import org.infernus.idea.checkstyle.config.PluginConfiguration;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.infernus.idea.checkstyle.util.CheckstyleDownloadHelper;
import org.infernus.idea.checkstyle.util.Strings;
import org.infernus.idea.checkstyle.util.ThirdPartyJarDownloadHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNullElseGet;


/**
 * Provides a configuration panel (dialog) for project-level configuration.
 */
public class CheckStyleConfigPanel extends JPanel {
    private static final Insets COMPONENT_INSETS = JBUI.insets(4);
    private static final int ACTIVE_COL_MIN_WIDTH = 40;
    private static final int ACTIVE_COL_MAX_WIDTH = 50;
    private static final int DESC_COL_MIN_WIDTH = 100;
    private static final int DESC_COL_MAX_WIDTH = 200;
    private static final Dimension DECORATOR_DIMENSIONS = new Dimension(300, 50);

    private final JList<String> pathList = new JBList<>(new DefaultListModel<>());

    private final JLabel csVersionDropdownLabel = new JLabel(CheckStyleBundle.message("config.csversion.labelText") + ":");
    private final ComboBox<String> csVersionDropdown;
    private final JLabel scopeDropdownLabel = new JLabel(CheckStyleBundle.message("config.scanscope.labelText") + ":");
    private final ComboBox<ScanScope> scopeDropdown = new ComboBox<>(ScanScope.values());
    private final JCheckBox suppressErrorsCheckbox = new JCheckBox();
    private final JCheckBox copyLibsCheckbox = new JCheckBox();
    private final JCheckBox importSettingsFromMavenCheckbox = new JCheckBox();
    private final JCheckBox scanBeforeCheckinCheckbox = new JCheckBox();

    private final LocationTableModel locationModel = new LocationTableModel();
    private final JBTable locationTable = new JBTable(locationModel);

    private boolean shownScanBeforeCheckin;

    private final Project project;
    private final CheckstyleProjectService checkstyleProjectService;
    private final CheckerFactoryCache checkerFactoryCache;
    private final PluginConfigurationManager pluginConfigurationManager;
    private final VersionListReader versionListReader;
    private final Path m2Root;
    private final Map<String, String> versionSuffixCache = new HashMap<>();
    private ThirdPartyJarCache thirdPartyJarCache = ThirdPartyJarCache.create();

    public CheckStyleConfigPanel(@NotNull final Project project) {
        super(new BorderLayout());

        this.project = project;

        this.checkstyleProjectService = project.getService(CheckstyleProjectService.class);
        this.checkerFactoryCache = project.getService(CheckerFactoryCache.class);
        this.pluginConfigurationManager = project.getService(PluginConfigurationManager.class);
        this.versionListReader = new VersionListReader();
        this.m2Root = CheckstyleArtifactDownloader.defaultM2Root();

        refreshVersionSuffixCache();
        csVersionDropdown = buildCheckstyleVersionComboBox();

        initialise();
    }

    private ComboBox<String> buildCheckstyleVersionComboBox() {
        SortedSet<String> versions = checkstyleProjectService.getSupportedVersions();
        SortedSet<String> reversedVersions = new TreeSet<>(Collections.reverseOrder(versions.comparator()));
        reversedVersions.addAll(versions);
        String[] reversed = reversedVersions.toArray(new String[0]);
        String[] allVersions = new String[reversed.length + 1];
        allVersions[0] = VersionListReader.LATEST_VERSION;
        System.arraycopy(reversed, 0, allVersions, 1, reversed.length);
        ComboBox<String> comboBox = new ComboBox<>(allVersions);
        comboBox.setRenderer(new VersionStatusRenderer());
        return comboBox;
    }

    private void refreshVersionSuffixCache() {
        for (String version : checkstyleProjectService.getSupportedVersions()) {
            String suffix;
            if (versionListReader.isBundled(version)) {
                suffix = " [bundled]";
            } else if (CheckstyleArtifactDownloader.isAvailableLocally(m2Root, version)) {
                suffix = " [downloaded]";
            } else {
                suffix = " ↓";
            }
            versionSuffixCache.put(version, suffix);
        }
    }

    private class VersionStatusRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                      final int index, final boolean isSelected,
                                                      final boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof String version) {
                if (versionListReader.isLatest(version)) {
                    final String resolved = versionListReader.getDefaultVersion();
                    setText(CheckStyleBundle.message("config.csversion.latest", resolved)
                            + versionSuffixCache.getOrDefault(resolved, ""));
                } else {
                    setText(version + versionSuffixCache.getOrDefault(version, ""));
                }
            }
            return this;
        }
    }

    private void activateCurrentClasspath() {
        checkerFactoryCache.invalidate();
        final String selectedVersion = getCheckstyleVersion();
        final String resolvedVersion = versionListReader.isLatest(selectedVersion)
                ? versionListReader.getDefaultVersion()
                : selectedVersion;

        if (!versionListReader.isBundled(resolvedVersion)) {
            CheckstyleArtifactDownloader downloader = checkstyleProjectService.getDownloader();
            if (downloader != null && !downloader.isAvailableLocally(resolvedVersion)) {
                if (!downloadWithProgress(resolvedVersion, downloader)) {
                    return;
                }
                refreshVersionSuffixCache();
                csVersionDropdown.repaint();
            }
        }
        // Re-read selected version: "Use bundled version" path may have changed the dropdown selection.
        checkstyleProjectService.activateCheckstyleVersion(getCheckstyleVersion(), getThirdPartyClasspath());
    }

    private boolean downloadWithProgress(@NotNull final String version,
                                         @NotNull final CheckstyleArtifactDownloader downloader) {
        return CheckstyleDownloadHelper.downloadWithProgress(project, version, downloader, versionListReader,
                csVersionDropdown::setSelectedItem);
    }

    private void initialise() {
        add(buildConfigPanel(), BorderLayout.CENTER);
    }

    private JPanel buildConfigPanel() {
        scopeDropdownLabel.setToolTipText(CheckStyleBundle.message("config.scanscope.tooltip"));
        scopeDropdown.setToolTipText(CheckStyleBundle.message("config.scanscope.tooltip"));

        suppressErrorsCheckbox.setText(CheckStyleBundle.message("config.suppress-errors.checkbox.text"));
        suppressErrorsCheckbox.setToolTipText(CheckStyleBundle.message("config.suppress-errors.checkbox.tooltip"));

        copyLibsCheckbox.setText(CheckStyleBundle.message("config.stabilize-classpath.text"));
        copyLibsCheckbox.setToolTipText(CheckStyleBundle.message("config.stabilize-classpath.tooltip"));

        importSettingsFromMavenCheckbox.setText(CheckStyleBundle.message("config.import-maven-settings.text"));
        importSettingsFromMavenCheckbox.setToolTipText(CheckStyleBundle.message("config.import-maven-settings.tooltip"));

        scanBeforeCheckinCheckbox.setText(CheckStyleBundle.message("config.scan-before-checkin.text"));
        scanBeforeCheckinCheckbox.setToolTipText(CheckStyleBundle.message("config.scan-before-checkin.tooltip"));

        final JPanel configFilePanel = new JPanel(new GridBagLayout());
        configFilePanel.setOpaque(false);

        configFilePanel.add(csVersionDropdownLabel, new GridBagConstraints(
                0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
        configFilePanel.add(csVersionDropdown, new GridBagConstraints(
                1, 0, 3, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
        configFilePanel.add(scopeDropdownLabel, new GridBagConstraints(
                0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
        configFilePanel.add(scopeDropdown, new GridBagConstraints(
                1, 1, 3, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
        configFilePanel.add(suppressErrorsCheckbox, new GridBagConstraints(
                0, 2, 2, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, COMPONENT_INSETS, 0, 0));
        configFilePanel.add(importSettingsFromMavenCheckbox, new GridBagConstraints(
                2, 2, 2, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, COMPONENT_INSETS, 0, 0));
        configFilePanel.add(copyLibsCheckbox, new GridBagConstraints(
                0, 3, 2, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, COMPONENT_INSETS, 0, 0));
        configFilePanel.add(scanBeforeCheckinCheckbox, new GridBagConstraints(
                2, 3, 2, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, COMPONENT_INSETS, 0, 0));
        configFilePanel.add(buildRuleFilePanel(), new GridBagConstraints(
                0, 4, 4, 1, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.BOTH, COMPONENT_INSETS, 0, 0));
        configFilePanel.add(buildClassPathPanel(), new GridBagConstraints(
                0, 5, 4, 1, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.BOTH, COMPONENT_INSETS, 0, 0));

        return configFilePanel;
    }

    private JPanel buildRuleFilePanel() {
        setColumnWith(locationTable, 0, ACTIVE_COL_MIN_WIDTH, ACTIVE_COL_MAX_WIDTH, ACTIVE_COL_MAX_WIDTH);
        setColumnWith(locationTable, 1, DESC_COL_MIN_WIDTH, DESC_COL_MAX_WIDTH, DESC_COL_MAX_WIDTH);
        locationTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        locationTable.setStriped(true);
        locationTable.getTableHeader().setReorderingAllowed(false);

        final ToolbarDecorator tableDecorator = ToolbarDecorator.createDecorator(locationTable);
        tableDecorator.setAddAction(new AddLocationAction());
        tableDecorator.setEditAction(new EditPropertiesAction());
        tableDecorator.setRemoveAction(new RemoveLocationAction());
        tableDecorator.setEditActionUpdater(new EnableWhenSelectedAndRemovable());
        tableDecorator.setRemoveActionUpdater(new EnableWhenSelectedAndRemovable());
        tableDecorator.addExtraAction((AnAction) new ExportLocationAction());
        tableDecorator.addExtraAction((AnAction) new DetectConventionalConfigurationAction());
        tableDecorator.setPreferredSize(DECORATOR_DIMENSIONS);

        final JPanel container = new JPanel(new BorderLayout());
        container.add(new TitledSeparator(CheckStyleBundle.message("config.file.tab")), BorderLayout.NORTH);
        container.add(tableDecorator.createPanel(), BorderLayout.CENTER);
        final JLabel infoLabel = new JLabel(CheckStyleBundle.message("config.file.description"),
                AllIcons.General.Information, SwingConstants.LEFT);
        infoLabel.setBorder(JBUI.Borders.empty(8, 0, 4, 0));
        container.add(infoLabel, BorderLayout.SOUTH);
        return container;
    }

    private JPanel buildClassPathPanel() {
        final ToolbarDecorator pathListDecorator = ToolbarDecorator.createDecorator(pathList);
        pathListDecorator.setAddAction(new AddPathAction());
        pathListDecorator.setEditAction(new EditPathAction());
        pathListDecorator.setRemoveAction(new RemovePathAction());
        pathListDecorator.setMoveUpAction(new MoveUpPathAction());
        pathListDecorator.setMoveDownAction(new MoveDownPathAction());
        pathListDecorator.setPreferredSize(DECORATOR_DIMENSIONS);

        final JPanel container = new JPanel(new BorderLayout());
        container.add(new TitledSeparator(CheckStyleBundle.message("config.path.tab")), BorderLayout.NORTH);
        container.add(pathListDecorator.createPanel(), BorderLayout.CENTER);
        return container;
    }

    private void setColumnWith(final JTable table,
                               final int columnIndex,
                               final int minSize,
                               final int preferredSize,
                               final Integer maxSize) {
        final TableColumn column = table.getColumnModel().getColumn(columnIndex);
        column.setMinWidth(minSize);
        column.setWidth(preferredSize);
        column.setPreferredWidth(preferredSize);
        if (maxSize != null) {
            column.setMaxWidth(maxSize);
        }
    }

    private void setThirdPartyClasspath(final List<String> classpath) {
        List<String> thirdPartyClasspath;
        thirdPartyClasspath = Objects.requireNonNullElseGet(classpath, ArrayList::new);

        final DefaultListModel<String> listModel = pathListModel();
        listModel.clear();

        for (final String classPathFile : thirdPartyClasspath) {
            if (!Strings.isBlank(classPathFile)) {
                listModel.addElement(classPathFile);
            }
        }
    }

    private String getCheckstyleVersion() {
        return (String) csVersionDropdown.getSelectedItem();
    }


    private DefaultListModel<String> pathListModel() {
        return (DefaultListModel<String>) pathList.getModel();
    }


    @NotNull
    private List<String> getThirdPartyClasspath() {
        final List<String> classpath = new ArrayList<>();

        final DefaultListModel<String> listModel = pathListModel();
        for (int i = 0; i < listModel.size(); ++i) {
            final String path = listModel.get(i);
            classpath.add(path);
        }

        return classpath;
    }


    public void showPluginConfiguration(@NotNull final PluginConfiguration pluginConfig) {
        csVersionDropdown.setSelectedItem(pluginConfig.getCheckstyleVersion());
        scopeDropdown.setSelectedItem(pluginConfig.getScanScope());
        suppressErrorsCheckbox.setSelected(pluginConfig.isSuppressErrors());
        copyLibsCheckbox.setSelected(pluginConfig.isCopyLibs());
        importSettingsFromMavenCheckbox.setSelected(pluginConfig.isImportSettingsFromMaven());
        setShownScanBeforeCheckin(pluginConfig);
        scanBeforeCheckinCheckbox.setSelected(shownScanBeforeCheckin);
        locationModel.setLocations(new ArrayList<>(pluginConfig.getLocations()));
        setThirdPartyClasspath(pluginConfig.getThirdPartyClasspath());
        locationModel.setActiveLocations(pluginConfig.getActiveLocations());
    }

    /**
     * IDEA does not reset us after an apply, so re-baseline what the checkbox was last shown as -
     * otherwise it counts as touched for the rest of the dialog session.
     */
    public void markAsApplied(@NotNull final PluginConfiguration appliedConfig) {
        setShownScanBeforeCheckin(appliedConfig);
    }

    private void setShownScanBeforeCheckin(@NotNull final PluginConfiguration pluginConfig) {
        shownScanBeforeCheckin = pluginConfig.isScanBeforeCheckin();
    }

    /**
     * The pre-commit scan setting is also editable on the Version Control &gt; Commit page, which the
     * settings tree applies before us. Carrying the live value through when our own checkbox was left
     * alone stops us discarding an edit made there in the same session.
     */
    private boolean scanBeforeCheckinToWriteBack(@NotNull final PluginConfiguration current) {
        if (scanBeforeCheckinCheckbox.isSelected() != shownScanBeforeCheckin) {
            return scanBeforeCheckinCheckbox.isSelected();
        }
        return current.isScanBeforeCheckin();
    }

    public JCheckBox getScanBeforeCheckinCheckbox() {
        return scanBeforeCheckinCheckbox;
    }

    public PluginConfiguration getPluginConfiguration() {
        final String checkstyleVersion = requireNonNullElseGet(
                (String) csVersionDropdown.getSelectedItem(),
                () -> new VersionListReader().getDefaultVersion());
        ScanScope scanScope = (ScanScope) scopeDropdown.getSelectedItem();
        if (scanScope == null) {
            scanScope = ScanScope.getDefaultValue();
        }

        final PluginConfiguration current = pluginConfigurationManager.getCurrent();

        return PluginConfigurationBuilder.from(current)
                .withCheckstyleVersion(checkstyleVersion)
                .withScanScope(scanScope)
                .withSuppressErrors(suppressErrorsCheckbox.isSelected())
                .withCopyLibraries(copyLibsCheckbox.isSelected())
                .withImportSettingsFromMaven(importSettingsFromMavenCheckbox.isSelected())
                .withScanBeforeCheckin(scanBeforeCheckinToWriteBack(current))
                .withLocations(new TreeSet<>(locationModel.getLocations()))
                .withThirdPartyClassPath(getThirdPartyClasspath())
                .withActiveLocationIds(locationModel.getActiveLocations().stream()
                        .map(ConfigurationLocation::getId)
                        .collect(Collectors.toCollection(TreeSet::new)))
                .build();
    }


    /**
     * Process the addition of a configuration location.
     */
    private final class AddLocationAction implements AnActionButtonRunnable {
        @Override
        public void run(final AnActionButton anActionButton) {
            final LocationDialogue dialogue = new LocationDialogue(
                    parentDialogue(),
                    project,
                    getCheckstyleVersion(),
                    getThirdPartyClasspath(),
                    checkstyleProjectService);

            if (dialogue.showAndGet()) {
                final ConfigurationLocation newLocation = dialogue.getConfigurationLocation();
                if (locationModel.getLocations().contains(newLocation)
                        || hasDuplicateBundledDescription(newLocation, null)) {
                    Messages.showWarningDialog(project,
                            CheckStyleBundle.message("config.file.error.duplicate.text"),
                            CheckStyleBundle.message("config.file.error.duplicate.title"));

                } else {
                    locationModel.addLocation(dialogue.getConfigurationLocation());
                }
            }
        }
    }

    private Dialog parentDialogue() {
        return (Dialog) SwingUtilities.getAncestorOfClass(Dialog.class, CheckStyleConfigPanel.this);
    }

    /**
     * Process the removal of a configuration location.
     */
    private final class RemoveLocationAction implements AnActionButtonRunnable {
        @Override
        public void run(final AnActionButton anActionButton) {
            final int selectedIndex = locationTable.getSelectedRow();
            if (selectedIndex == -1) {
                return;
            }

            locationModel.removeLocationAt(selectedIndex);
        }
    }

    /**
     * Edit the properties of a configuration location.
     */
    private final class EditPropertiesAction implements AnActionButtonRunnable {
        @Override
        public void run(final AnActionButton anActionButton) {
            final int selectedIndex = locationTable.getSelectedRow();
            if (selectedIndex == -1) {
                return;
            }

            final ConfigurationLocation location = locationModel.getLocationAt(selectedIndex);

            final PropertiesDialogue propertiesDialogue = new PropertiesDialogue(
                    parentDialogue(), project, checkstyleProjectService);
            propertiesDialogue.setConfigurationLocation(location);

            if (propertiesDialogue.showAndGet()) {
                final ConfigurationLocation editedLocation = propertiesDialogue.getConfigurationLocation();
                if (wouldCollideOnEdit(location, editedLocation)) {
                    Messages.showWarningDialog(project,
                            CheckStyleBundle.message("config.file.error.duplicate.text"),
                            CheckStyleBundle.message("config.file.error.duplicate.title"));

                } else {
                    locationModel.updateLocation(location, editedLocation);
                }
            }
        }
    }

    /**
     * Export the resolved XML of a configuration location to a file.
     */
    private final class ExportLocationAction extends AnActionButton {
        ExportLocationAction() {
            super(CheckStyleBundle.message("config.file.export.text"), AllIcons.ToolbarDecorator.Export);
            addCustomUpdater(e -> locationTable.getSelectedRow() >= 0);
        }

        @Override
        public void actionPerformed(@NotNull final AnActionEvent e) {
            final int selectedIndex = locationTable.getSelectedRow();
            if (selectedIndex == -1) {
                return;
            }

            final ConfigurationLocation location = locationModel.getLocationAt(selectedIndex);

            final FileSaverDescriptor descriptor = new FileSaverDescriptor(
                    CheckStyleBundle.message("config.file.export.text"),
                    CheckStyleBundle.message("config.file.export.tooltip"),
                    "xml");
            final FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project);
            final VirtualFileWrapper wrapper = dialog.save(
                    ProjectUtil.guessProjectDir(project), ConfigurationExporter.suggestedFileName(location));
            if (wrapper == null) {
                return;
            }

            final File destination = wrapper.getFile();
            final Exception[] failure = {null};
            ProgressManager.getInstance().run(new Task.Modal(
                    project, CheckStyleBundle.message("config.export.title", location.getDescription()), false) {
                @Override
                public void run(@NotNull final ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    try {
                        ConfigurationExporter.export(location, checkstyleProjectService.underlyingClassLoader(), destination);
                    } catch (Exception exception) {
                        failure[0] = exception;
                    }
                }
            });

            if (failure[0] != null) {
                Messages.showErrorDialog(project,
                        CheckStyleBundle.message("config.export.failed", Objects.toString(
                                failure[0].getMessage(), failure[0].getClass().getSimpleName())),
                        CheckStyleBundle.message("config.export.failed.title"));
                return;
            }

            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(destination);

            String message = CheckStyleBundle.message("config.export.success", destination.getAbsolutePath());
            if (ConfigurationExporter.hasConfiguredProperties(location)) {
                message += " " + CheckStyleBundle.message("config.export.success.properties-warning");
            }
            Messages.showInfoMessage(project, message, CheckStyleBundle.message("config.export.success.title"));
        }
    }

    /**
     * Scan for a Checkstyle configuration file at one of a fixed set of conventional, project-relative
     * locations and merge the result into this panel's in-memory location table. Mirrors
     * {@link DetectConventionalConfigurationLocation}, but operates on {@link #locationModel} instead of
     * writing straight to {@link PluginConfigurationManager}, so that Cancel still discards it.
     */
    private final class DetectConventionalConfigurationAction extends AnActionButton {
        DetectConventionalConfigurationAction() {
            super(CheckStyleBundle.message("config.file.detect.text"), AllIcons.Actions.Find);
            getTemplatePresentation().setDescription(CheckStyleBundle.message("config.file.detect.tooltip"));
        }

        @Override
        public void actionPerformed(@NotNull final AnActionEvent e) {
            final List<ConfigurationLocation> snapshot = List.copyOf(locationModel.getLocations());
            final ScanResult[] result = new ScanResult[1];
            ProgressManager.getInstance().run(new Task.Modal(
                    project, CheckStyleBundle.message("detect.title"), false) {
                @Override
                public void run(@NotNull final ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    result[0] = ConventionalConfigurationLocationScanner.scan(project, snapshot);
                }
            });

            applyDetectionResult(result[0]);
            DetectConventionalConfigurationLocation.messageFor(result[0].outcome())
                    .ifPresent(msg -> Messages.showInfoMessage(
                            project, msg, CheckStyleBundle.message("config.file.detect.text")));
        }
    }

    /**
     * Applies a {@link ScanResult} to {@link #locationModel}: replaces the location list and, if a
     * location was detected, (re-)activates it. Package-private as a test seam, so tests can drive it
     * directly on the EDT without going through {@link Task.Modal}.
     */
    void applyDetectionResult(@NotNull final ScanResult result) {
        if (result.outcome() == ScanOutcome.NO_PROJECT_DIRECTORY) {
            return;
        }

        locationModel.setLocations(result.locations());

        result.found().ifPresent(location -> {
            final SortedSet<ConfigurationLocation> active = new TreeSet<>(locationModel.getActiveLocations());
            active.add(location);
            locationModel.setActiveLocations(active);
        });
    }

    /**
     * Replaces this panel's {@link ThirdPartyJarCache} with one supplied by a test, independent of
     * {@link #checkstyleProjectService}'s own instance (see the "two independent instances" design
     * note in plan-586.md).
     */
    void setThirdPartyJarCacheForTesting(@NotNull final ThirdPartyJarCache thirdPartyJarCache) {
        this.thirdPartyJarCache = thirdPartyJarCache;
    }

    /**
     * Adds {@code url} to the classpath list and activates it. Assumes {@code url} has already been
     * successfully fetched by something else (in production, {@link org.infernus.idea.checkstyle.util.ThirdPartyJarDownloadHelper});
     * this method itself does no network I/O and only touches the Swing list model, so it is safe to
     * call directly from the EDT. Package-private as a test seam.
     */
    void applyUrlClasspathEntry(@NotNull final String url) {
        final DefaultListModel<String> listModel = pathListModel();
        if (!listModel.contains(url)) {
            listModel.addElement(url);
        }
        activateCurrentClasspath();
    }

    /**
     * Process the addition of a path element.
     */
    private final class AddPathAction implements AnActionButtonRunnable {
        @Override
        public void run(final AnActionButton anActionButton) {
            final String fileLabel = CheckStyleBundle.message("config.path.add.choice.file");
            final String urlLabel = CheckStyleBundle.message("config.path.add.choice.url");
            final String cancelLabel = CommonBundle.getCancelButtonText();
            final int choice = Messages.showDialog(project,
                    CheckStyleBundle.message("config.path.add.choice.message"),
                    CheckStyleBundle.message("config.path.add.choice.title"),
                    new String[]{fileLabel, urlLabel, cancelLabel}, 0, Messages.getQuestionIcon());
            if (choice == 0) {
                runAddFile();
            } else if (choice == 1) {
                runAddUrl();
            }
        }

        private void runAddFile() {
            final VirtualFile chosen = FileChooser.chooseFile(checkStyleRulesFileChooserDescriptor(),
                    CheckStyleConfigPanel.this, project, ProjectUtil.guessProjectDir(project));
            if (chosen != null) {
                (pathListModel()).addElement(
                        VfsUtilCore.virtualToIoFile(chosen).getAbsolutePath());
                activateCurrentClasspath();
            }
        }

        private void runAddUrl() {
            String url = null;
            while (true) {
                url = Messages.showInputDialog(project,
                        CheckStyleBundle.message("config.path.add.url.prompt"),
                        CheckStyleBundle.message("config.path.add.url.title"),
                        Messages.getQuestionIcon(), url, null);
                if (url == null) {
                    return;
                }
                if (Strings.isHttpUrl(url)) {
                    break;
                }
                Messages.showErrorDialog(project,
                        CheckStyleBundle.message("config.path.add.url.invalid"),
                        CheckStyleBundle.message("config.path.add.url.title"));
            }

            if (pathListModel().contains(url)) {
                Messages.showWarningDialog(project,
                        CheckStyleBundle.message("config.path.add.duplicate.text"),
                        CheckStyleBundle.message("config.path.add.duplicate.title"));
                return;
            }

            if (ThirdPartyJarDownloadHelper.forceRefreshWithProgress(project, url, thirdPartyJarCache)) {
                applyUrlClasspathEntry(url);
            }
        }
    }

    private FileChooserDescriptor checkStyleRulesFileChooserDescriptor() {
        return new FileChooserDescriptor(true, false, true, true, false, false)
                .withFileFilter((file) -> {
                    final String currentExtension = file.getExtension();
                    return currentExtension != null && "jar".equalsIgnoreCase(currentExtension.trim());
                });
    }

    /**
     * Process the editing of a path element.
     */
    private final class EditPathAction implements AnActionButtonRunnable {
        @Override
        public void run(final AnActionButton anActionButton) {
            final int selected = pathList.getSelectedIndex();
            if (selected < 0) {
                return;
            }

            final DefaultListModel<String> listModel = pathListModel();
            final String selectedFile = listModel.get(selected);

            final VirtualFile toSelect = LocalFileSystem.getInstance().findFileByPath(selectedFile);
            final VirtualFile chosen = FileChooser.chooseFile(checkStyleRulesFileChooserDescriptor(), project, toSelect);
            if (chosen != null) {
                listModel.remove(selected);
                listModel.add(selected, VfsUtilCore.virtualToIoFile(chosen).getAbsolutePath());
                pathList.setSelectedIndex(selected);
                activateCurrentClasspath();
            }
        }
    }

    /**
     * Process the removal of a path element.
     */
    private final class RemovePathAction implements AnActionButtonRunnable {
        @Override
        public void run(final AnActionButton anActionButton) {
            final int[] selected = pathList.getSelectedIndices();
            if (selected == null || selected.length == 0) {
                return;
            }

            for (final int index : selected) {
                (pathListModel()).remove(index);
            }
            activateCurrentClasspath();
        }
    }

    /**
     * Process the move up of a path element.
     */
    private final class MoveUpPathAction implements AnActionButtonRunnable {
        @Override
        public void run(final AnActionButton anActionButton) {
            final int selected = pathList.getSelectedIndex();
            if (selected < 1) {
                return;
            }

            final DefaultListModel<String> listModel = pathListModel();
            final String element = listModel.remove(selected);
            listModel.add(selected - 1, element);

            pathList.setSelectedIndex(selected - 1);
        }
    }

    /**
     * Process the move down of a path element.
     */
    private final class MoveDownPathAction implements AnActionButtonRunnable {
        @Override
        public void run(final AnActionButton anActionButton) {
            final DefaultListModel<String> listModel = pathListModel();
            final int selected = pathList.getSelectedIndex();
            if (selected == -1 || selected == (listModel.getSize() - 1)) {
                return;
            }

            final String element = listModel.remove(selected);
            listModel.add(selected + 1, element);

            pathList.setSelectedIndex(selected + 1);
        }
    }

    boolean isEditOrRemoveEnabledFor(final int row) {
        return row >= 0 && locationModel.getLocationAt(row).isRemovable();
    }

    /**
     * Would saving {@code edited} in place of {@code original} give it the same description as a
     * different, already-present bundled location? {@code original} itself is excluded, so an edit that
     * leaves the description unchanged is never flagged.
     * <p>Descriptions, not {@link ConfigurationLocation#equals}, are the right comparison here: two
     * bundled copies of the same style are deliberately distinguished by id (so a rename never silently
     * clobbers a distinct entry), but that same id-based distinction means a plain equals() check would
     * miss the user visibly renaming one copy to match another row's description. Non-bundled locations
     * are unaffected: {@link #hasDuplicateBundledDescription} only compares {@code BUNDLED} entries.</p>
     */
    boolean wouldCollideOnEdit(final ConfigurationLocation original, final ConfigurationLocation edited) {
        return hasDuplicateBundledDescription(edited, original);
    }

    /**
     * Would {@code candidate} share its description with a different bundled location already in the
     * table? Only {@link ConfigurationType#BUNDLED} locations are compared: two bundled copies of the
     * same style are otherwise indistinguishable to the user except by description, whereas non-bundled
     * locations (file/HTTP/classpath) have always been allowed to share a description while pointing at
     * different underlying locations.
     *
     * @param candidate the location to check.
     * @param excluding a location to exclude from the comparison (e.g. the original being edited), or
     *                  {@code null} if there is none.
     */
    boolean hasDuplicateBundledDescription(final ConfigurationLocation candidate, @Nullable final ConfigurationLocation excluding) {
        if (candidate.getType() != ConfigurationType.BUNDLED) {
            return false;
        }
        return locationModel.getLocations().stream()
                .anyMatch(existing -> existing != candidate
                        && existing != excluding
                        && existing.getType() == ConfigurationType.BUNDLED
                        && java.util.Objects.equals(existing.getDescription(), candidate.getDescription()));
    }

    LocationTableModel locationModel() {
        return locationModel;
    }

    private final class EnableWhenSelectedAndRemovable implements AnActionButtonUpdater {
        @Override
        public boolean isEnabled(@NotNull final AnActionEvent e) {
            return isEditOrRemoveEnabledFor(locationTable.getSelectedRow());
        }
    }
}
