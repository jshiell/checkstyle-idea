package org.infernus.idea.checkstyle.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableColumn;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.AnActionButtonUpdater;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.checker.CheckerFactoryCache;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.infernus.idea.checkstyle.util.Icons;
import org.infernus.idea.checkstyle.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides a configuration panel for project-level configuration.
 */
public class CheckStyleConfigPanel extends JPanel {
    private static final Insets COMPONENT_INSETS = JBUI.insets(4);
    private static final int ACTIVE_COL_MIN_WIDTH = 40;
    private static final int ACTIVE_COL_MAX_WIDTH = 50;
    private static final int DESC_COL_MIN_WIDTH = 100;
    private static final int DESC_COL_MAX_WIDTH = 200;
    private static final Dimension DECORATOR_DIMENSIONS = new Dimension(300, 50);

    private static final String SUN_CHECKS_CONFIG = "/sun_checks.xml";
    private final List<ConfigurationLocation> presetLocations;

    private final JList pathList = new JBList(new DefaultListModel<String>());

    private final JLabel csVersionDropdownLabel = new JLabel(CheckStyleBundle.message("config.csversion.labelText") + ":");
    private final ComboBox csVersionDropdown;
    private final JLabel scopeDropdownLabel = new JLabel(CheckStyleBundle.message("config.scanscope.labelText") + ":");
    private final ComboBox scopeDropdown = new ComboBox(ScanScope.values());
    private final JCheckBox suppressErrorsCheckbox = new JCheckBox();

    private final LocationTableModel locationModel = new LocationTableModel();
    private final JBTable locationTable = new JBTable(locationModel);

    private final Project project;


    public CheckStyleConfigPanel(@NotNull final Project project) {
        super(new BorderLayout());

        this.project = project;
        this.presetLocations = buildPresetLocations();
        this.csVersionDropdown = buildCheckstyleVersionComboBox(project);

        initialise();
    }

    private List<ConfigurationLocation> buildPresetLocations() {
        final ConfigurationLocationFactory locationFactory = getConfigurationLocationFactory();
        final List<ConfigurationLocation> result = new ArrayList<>();
        result.add(locationFactory.create(BundledConfig.SUN_CHECKS));
        result.add(locationFactory.create(BundledConfig.GOOGLE_CHECKS));
        return Collections.unmodifiableList(result);
    }

    private ComboBox buildCheckstyleVersionComboBox(@NotNull final Project currentProject) {
        SortedSet<String> versions = CheckstyleProjectService.getInstance(currentProject).getSupportedVersions();
        String[] supportedVersions = versions.toArray(new String[versions.size()]);
        return new ComboBox(supportedVersions);
    }

    private void activateCurrentClasspath() {
        ServiceManager.getService(CheckerFactoryCache.class).invalidate();

        CheckstyleProjectService csService = CheckstyleProjectService.getInstance(project);
        csService.activateCheckstyleVersion(getCheckstyleVersion(), getThirdPartyClasspath());
    }

    private ConfigurationLocationFactory getConfigurationLocationFactory() {
        return ServiceManager.getService(project, ConfigurationLocationFactory.class);
    }

    private void initialise() {
        add(buildConfigPanel(), BorderLayout.CENTER);
    }

    private JPanel buildConfigPanel() {
        scopeDropdownLabel.setToolTipText(CheckStyleBundle.message("config.scanscope.tooltip"));
        scopeDropdown.setToolTipText(CheckStyleBundle.message("config.scanscope.tooltip"));

        suppressErrorsCheckbox.setText(CheckStyleBundle.message("config.suppress-errors.checkbox.text"));
        suppressErrorsCheckbox.setToolTipText(CheckStyleBundle.message("config.suppress-errors.checkbox.tooltip"));

        final JPanel configFilePanel = new JPanel(new GridBagLayout());
        configFilePanel.setOpaque(false);

        configFilePanel.add(csVersionDropdownLabel, new GridBagConstraints(
                0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
        configFilePanel.add(csVersionDropdown, new GridBagConstraints(
                1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
        configFilePanel.add(scopeDropdownLabel, new GridBagConstraints(
                2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
        configFilePanel.add(scopeDropdown, new GridBagConstraints(
                3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
        configFilePanel.add(suppressErrorsCheckbox, new GridBagConstraints(
                4, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, COMPONENT_INSETS, 0, 0));
        configFilePanel.add(buildRuleFilePanel(), new GridBagConstraints(
                0, 1, 5, 1, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.BOTH, COMPONENT_INSETS, 0, 0));
        configFilePanel.add(buildClassPathPanel(), new GridBagConstraints(
                0, 2, 5, 1, 1.0, 1.0, GridBagConstraints.WEST,
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
        tableDecorator.setEditActionUpdater(new DisableForDefaultUpdater());
        tableDecorator.setRemoveActionUpdater(new DisableForDefaultUpdater());
        tableDecorator.setPreferredSize(DECORATOR_DIMENSIONS);

        final JPanel container = new JPanel(new BorderLayout());
        container.add(new TitledSeparator(CheckStyleBundle.message("config.file.tab")), BorderLayout.NORTH);
        container.add(tableDecorator.createPanel(), BorderLayout.CENTER);
        final JLabel infoLabel = new JLabel(CheckStyleBundle.message("config.file.description"),
                Icons.icon("/general/information.png"), SwingConstants.LEFT);
        infoLabel.setBorder(new EmptyBorder(8, 0, 4, 0));
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

    public void setScanScope(@Nullable final ScanScope pScanScope) {
        scopeDropdown.setSelectedItem(pScanScope != null ? pScanScope : ScanScope.getDefaultValue());
    }

    @NotNull
    public ScanScope getScanScope() {
        ScanScope scope = (ScanScope) scopeDropdown.getSelectedItem();
        return scope != null ? scope : ScanScope.getDefaultValue();
    }

    public void setSuppressingErrors(final boolean suppressingErrors) {
        suppressErrorsCheckbox.setSelected(suppressingErrors);
    }

    public boolean isSuppressingErrors() {
        return suppressErrorsCheckbox.isSelected();
    }

    /**
     * Set the third party classpath.
     *
     * @param classpath the third party classpath.
     */
    public void setThirdPartyClasspath(final List<String> classpath) {
        List<String> thirdPartyClasspath;
        if (classpath == null) {
            thirdPartyClasspath = new ArrayList<>();
        } else {
            thirdPartyClasspath = classpath;
        }

        final DefaultListModel<String> listModel = pathListModel();
        listModel.clear();

        for (final String classPathFile : thirdPartyClasspath) {
            if (!Strings.isBlank(classPathFile)) {
                listModel.addElement(classPathFile);
            }
        }
    }

    public String getCheckstyleVersion() {
        return (String) csVersionDropdown.getSelectedItem();
    }

    public void setCheckstyleVersion(@NotNull final String pVersion) {
        csVersionDropdown.setSelectedItem(pVersion);
    }


    @SuppressWarnings("unchecked")
    private DefaultListModel<String> pathListModel() {
        return (DefaultListModel<String>) pathList.getModel();
    }

    /**
     * Get the third party classpath.
     *
     * @return the third party classpath.
     */
    @NotNull
    public List<String> getThirdPartyClasspath() {
        final List<String> classpath = new ArrayList<>();

        final DefaultListModel listModel = pathListModel();
        for (int i = 0; i < listModel.size(); ++i) {
            final String path = (String) listModel.get(i);
            classpath.add(path);
        }

        return classpath;
    }

    public List<ConfigurationLocation> getConfigurationLocations() {
        return Collections.unmodifiableList(locationModel.getLocations());
    }

    public void setConfigurationLocations(final List<ConfigurationLocation> newLocations) {
        final List<ConfigurationLocation> modelLocations = new ArrayList<>(newLocations);
        Collections.sort(modelLocations);
        ensurePresetLocations(modelLocations);
        locationModel.setLocations(modelLocations);
    }

    private void ensurePresetLocations(final List<ConfigurationLocation> locations) {
        presetLocations.stream()
                .filter(presetLocation -> !locations.contains(presetLocation))
                .forEach(presetLocation -> locations.add(0, presetLocation));
    }

    public void setActiveLocation(final ConfigurationLocation activeLocation) {
        locationModel.setActiveLocation(activeLocation);
    }

    public ConfigurationLocation getActiveLocation() {
        return locationModel.getActiveLocation();
    }


    /**
     * Process the addition of a configuration location.
     */
    private final class AddLocationAction extends ToolbarAction {
        private static final long serialVersionUID = -7266120887003483814L;

        AddLocationAction() {
            putValue(Action.NAME, CheckStyleBundle.message("config.file.add.text"));
            putValue(Action.SHORT_DESCRIPTION, CheckStyleBundle.message("config.file.add.tooltip"));
            putValue(Action.LONG_DESCRIPTION, CheckStyleBundle.message("config.file.add.tooltip"));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            final LocationDialogue dialogue = new LocationDialogue(project);

            dialogue.setVisible(true);

            if (dialogue.isCommitted()) {
                final ConfigurationLocation newLocation = dialogue.getConfigurationLocation();
                if (locationModel.getLocations().contains(newLocation)) {
                    Messages.showWarningDialog(project,
                            CheckStyleBundle.message("config.file.error.duplicate.text"),
                            CheckStyleBundle.message("config.file.error.duplicate.title"));

                } else {
                    locationModel.addLocation(dialogue.getConfigurationLocation());
                }
            }
        }
    }

    /**
     * Process the removal of a configuration location.
     */
    private final class RemoveLocationAction extends ToolbarAction {
        private static final long serialVersionUID = -799542186049804472L;

        RemoveLocationAction() {
            putValue(Action.NAME, CheckStyleBundle.message("config.file.remove.text"));
            putValue(Action.SHORT_DESCRIPTION, CheckStyleBundle.message("config.file.remove.tooltip"));
            putValue(Action.LONG_DESCRIPTION, CheckStyleBundle.message("config.file.remove.tooltip"));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
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
    private final class EditPropertiesAction extends ToolbarAction {
        private static final long serialVersionUID = -799542186049804472L;

        EditPropertiesAction() {
            putValue(Action.NAME, CheckStyleBundle.message("config.file.properties.text"));
            putValue(Action.SHORT_DESCRIPTION, CheckStyleBundle.message("config.file.properties.tooltip"));
            putValue(Action.LONG_DESCRIPTION, CheckStyleBundle.message("config.file.properties.tooltip"));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            final int selectedIndex = locationTable.getSelectedRow();
            if (selectedIndex == -1) {
                return;
            }

            final ConfigurationLocation location = locationModel.getLocationAt(selectedIndex);

            final PropertiesDialogue propertiesDialogue = new PropertiesDialogue(project);
            propertiesDialogue.setConfigurationLocation(location);

            propertiesDialogue.setVisible(true);

            if (propertiesDialogue.isCommitted()) {
                final ConfigurationLocation editedLocation = propertiesDialogue.getConfigurationLocation();
                locationModel.updateLocation(location, editedLocation);
            }
        }
    }

    abstract class ToolbarAction extends AbstractAction implements AnActionButtonRunnable {
        private static final long serialVersionUID = 7091312536206510956L;

        @Override
        public void run(final AnActionButton anActionButton) {
            actionPerformed(null);
        }
    }

    /**
     * Process the addition of a path element.
     */
    private final class AddPathAction extends ToolbarAction {
        private static final long serialVersionUID = -1389576037231727360L;

        /**
         * Create a new add path action.
         */
        AddPathAction() {
            putValue(Action.NAME, CheckStyleBundle.message("config.path.add.text"));
            putValue(Action.SHORT_DESCRIPTION, CheckStyleBundle.message("config.path.add.tooltip"));
            putValue(Action.LONG_DESCRIPTION, CheckStyleBundle.message("config.path.add.tooltip"));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            final FileChooserDescriptor descriptor = new ExtensionFileChooserDescriptor(
                    (String) getValue(Action.NAME),
                    (String) getValue(Action.SHORT_DESCRIPTION),
                    false, "jar");
            final VirtualFile chosen = FileChooser.chooseFile(descriptor, project, project.getBaseDir());
            if (chosen != null) {
                (pathListModel()).addElement(
                        VfsUtilCore.virtualToIoFile(chosen).getAbsolutePath());
                activateCurrentClasspath();
            }
        }
    }

    /**
     * Process the editing of a path element.
     */
    private final class EditPathAction extends ToolbarAction {
        private static final long serialVersionUID = -1455378231580505750L;

        /**
         * Create a new edit path action.
         */
        EditPathAction() {
            putValue(Action.NAME, CheckStyleBundle.message("config.path.edit.text"));
            putValue(Action.SHORT_DESCRIPTION, CheckStyleBundle.message("config.path.edit.tooltip"));
            putValue(Action.LONG_DESCRIPTION, CheckStyleBundle.message("config.path.edit.tooltip"));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            final int selected = pathList.getSelectedIndex();
            if (selected < 0) {
                return;
            }

            final DefaultListModel<String> listModel = pathListModel();
            final String selectedFile = listModel.get(selected);

            final FileChooserDescriptor descriptor = new ExtensionFileChooserDescriptor(
                    (String) getValue(Action.NAME),
                    (String) getValue(Action.SHORT_DESCRIPTION),
                    false, "jar");
            final VirtualFile toSelect = LocalFileSystem.getInstance().findFileByPath(selectedFile);
            final VirtualFile chosen = FileChooser.chooseFile(descriptor, project, toSelect);
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
    private final class RemovePathAction extends ToolbarAction {
        private static final long serialVersionUID = 7339136485307147623L;

        /**
         * Create a new add path action.
         */
        RemovePathAction() {
            putValue(Action.NAME, CheckStyleBundle.message("config.path.remove.text"));
            putValue(Action.SHORT_DESCRIPTION, CheckStyleBundle.message("config.path.remove.tooltip"));
            putValue(Action.LONG_DESCRIPTION, CheckStyleBundle.message("config.path.remove.tooltip"));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
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
    private final class MoveUpPathAction extends ToolbarAction {
        private static final long serialVersionUID = -1230778908605654344L;

        /**
         * Create a new move-up path action.
         */
        MoveUpPathAction() {
            putValue(Action.NAME, CheckStyleBundle.message("config.path.move-up.text"));
            putValue(Action.SHORT_DESCRIPTION, CheckStyleBundle.message("config.path.move-up.tooltip"));
            putValue(Action.LONG_DESCRIPTION, CheckStyleBundle.message("config.path.move-up.tooltip"));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
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
    private final class MoveDownPathAction extends ToolbarAction {
        private static final long serialVersionUID = 1222511743014969175L;

        /**
         * Create a new move-down path action.
         */
        MoveDownPathAction() {
            putValue(Action.NAME, CheckStyleBundle.message("config.path.move-down.text"));
            putValue(Action.SHORT_DESCRIPTION, CheckStyleBundle.message("config.path.move-down.tooltip"));
            putValue(Action.LONG_DESCRIPTION, CheckStyleBundle.message("config.path.move-down.tooltip"));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
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

    private class DisableForDefaultUpdater implements AnActionButtonUpdater {
        @Override
        public boolean isEnabled(final AnActionEvent e) {
            final int selectedItem = locationTable.getSelectedRow();
            return selectedItem == -1
                    || !presetLocations.contains(locationModel.getLocationAt(selectedItem));
        }
    }
}
