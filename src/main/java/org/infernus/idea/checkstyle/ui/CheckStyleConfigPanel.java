package org.infernus.idea.checkstyle.ui;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.infernus.idea.checkstyle.CheckStyleConstants;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.util.IDEAUtilities;
import org.infernus.idea.checkstyle.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Provides a configuration panel for project-level configuration.
 */
public final class CheckStyleConfigPanel extends JPanel {
    private final JList pathList = new JBList(new DefaultListModel());

    private final JCheckBox testClassesCheckbox = new JCheckBox();
    private final JCheckBox scanNonJavaFilesCheckbox = new JCheckBox();
    private final JCheckBox suppressErrorsCheckbox = new JCheckBox();

    private final LocationTableModel locationModel = new LocationTableModel();
    private final JBTable locationTable = new JBTable(locationModel);

    private final Project project;

    private boolean scanTestClasses;
    private boolean scanNonJavaFiles;
    private boolean suppressingErrors;
    private List<String> thirdPartyClasspath;
    private List<ConfigurationLocation> locations;
    private ConfigurationLocation activeLocation;

    private ConfigurationLocation defaultLocation;

    public CheckStyleConfigPanel(@NotNull final Project project) {
        super(new BorderLayout());

        this.project = project;

        initialise();
    }

    private void initialise() {
        add(buildConfigPanel(), BorderLayout.CENTER);
    }

    private JPanel buildConfigPanel() {
        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);

        testClassesCheckbox.setText(resources.getString("config.test-classes.checkbox.text"));
        testClassesCheckbox.setToolTipText(resources.getString("config.test-classes.checkbox.tooltip"));

        scanNonJavaFilesCheckbox.setText(resources.getString("config.scan-nonjava-files.checkbox.text"));
        scanNonJavaFilesCheckbox.setToolTipText(resources.getString("config.scan-nonjava-files.checkbox.tooltip"));

        suppressErrorsCheckbox.setText(resources.getString("config.suppress-errors.checkbox.text"));
        suppressErrorsCheckbox.setToolTipText(resources.getString("config.suppress-errors.checkbox.tooltip"));

        final JPanel configFilePanel = new JPanel(new GridBagLayout());
        configFilePanel.setOpaque(false);

        configFilePanel.add(testClassesCheckbox, new GridBagConstraints(
                0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
        configFilePanel.add(scanNonJavaFilesCheckbox, new GridBagConstraints(
                1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
        configFilePanel.add(suppressErrorsCheckbox, new GridBagConstraints(
                2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
        configFilePanel.add(buildRuleFilePanel(resources), new GridBagConstraints(
                0, 1, 3, 1, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0));
        configFilePanel.add(buildClassPathPanel(resources), new GridBagConstraints(
                0, 2, 3, 1, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0));

        return configFilePanel;
    }

    private JPanel buildRuleFilePanel(final ResourceBundle resources) {
        setColumnWith(locationTable, 0, 40, 50, 50);
        setColumnWith(locationTable, 1, 100, 200, 200);
        locationTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        locationTable.setStriped(true);
        locationTable.getTableHeader().setReorderingAllowed(false);

        final ToolbarDecorator tableDecorator = ToolbarDecorator.createDecorator(locationTable);
        tableDecorator.setAddAction(new AddLocationAction());
        tableDecorator.setEditAction(new EditPropertiesAction());
        tableDecorator.setRemoveAction(new RemoveLocationAction());
        tableDecorator.setEditActionUpdater(new DisableForDefaultUpdater());
        tableDecorator.setRemoveActionUpdater(new DisableForDefaultUpdater());
        tableDecorator.setPreferredSize(new Dimension(300, 50));

        final JPanel container = new JPanel(new BorderLayout());
        container.add(new TitledSeparator(resources.getString("config.file.tab")), BorderLayout.NORTH);
        container.add(tableDecorator.createPanel(), BorderLayout.CENTER);
        final JLabel infoLabel = new JLabel(resources.getString("config.file.description"),
                IDEAUtilities.getIcon("/general/information.png"), SwingConstants.LEFT);
        infoLabel.setBorder(new EmptyBorder(8, 0, 4, 0));
        container.add(infoLabel, BorderLayout.SOUTH);
        return container;
    }

    private JPanel buildClassPathPanel(final ResourceBundle resources) {
        final ToolbarDecorator pathListDecorator = ToolbarDecorator.createDecorator(pathList);
        pathListDecorator.setAddAction(new AddPathAction());
        pathListDecorator.setEditAction(new EditPathAction());
        pathListDecorator.setRemoveAction(new RemovePathAction());
        pathListDecorator.setMoveUpAction(new MoveUpPathAction());
        pathListDecorator.setMoveDownAction(new MoveDownPathAction());
        pathListDecorator.setPreferredSize(new Dimension(300, 50));

        final JPanel container = new JPanel(new BorderLayout());
        container.add(new TitledSeparator(resources.getString("config.path.tab")), BorderLayout.NORTH);
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

    /**
     * Should we scan test classes?
     *
     * @param scanTestClasses true to scan test classes.
     */
    public void setScanTestClasses(final boolean scanTestClasses) {
        this.scanTestClasses = scanTestClasses;
        testClassesCheckbox.setSelected(scanTestClasses);
    }

    /**
     * Determine if we should scan test classes.
     *
     * @return true if test classes should be scanned.
     */
    public boolean isScanTestClasses() {
        scanTestClasses = testClassesCheckbox.isSelected();
        return scanTestClasses;
    }

    /**
     * Should we scan non-Java files?
     *
     * @param scanNonJavaFiles true to scan all files types, false to scan only Java files.
     */
    public void setScanNonJavaFiles(final boolean scanNonJavaFiles) {
        this.scanNonJavaFiles = scanNonJavaFiles;
        scanNonJavaFilesCheckbox.setSelected(scanNonJavaFiles);
    }

    /**
     * Determine if we should scan non-Java files.
     *
     * @return true if non-Java classes should be scanned.
     */
    public boolean isScanNonJavaFiles() {
        scanNonJavaFiles = scanNonJavaFilesCheckbox.isSelected();
        return scanNonJavaFiles;
    }

    public void setSuppressingErrors(final boolean suppressingErrors) {
        this.suppressingErrors = suppressingErrors;
        suppressErrorsCheckbox.setSelected(suppressingErrors);
    }

    public boolean isSuppressingErrors() {
        suppressingErrors = suppressErrorsCheckbox.isSelected();
        return suppressingErrors;
    }

    /**
     * Set the third party classpath.
     *
     * @param classpath the third party classpath.
     */
    public void setThirdPartyClasspath(final List<String> classpath) {
        if (classpath == null) {
            thirdPartyClasspath = new ArrayList<String>();
        } else {
            thirdPartyClasspath = classpath;
        }

        final DefaultListModel listModel = (DefaultListModel) pathList.getModel();
        listModel.clear();

        for (final String classPathFile : thirdPartyClasspath) {
            if (classPathFile != null && classPathFile.trim().length() > 0) {
                listModel.addElement(classPathFile);
            }
        }
    }

    /**
     * Get the third party classpath.
     *
     * @return the third party classpath.
     */
    @NotNull
    public List<String> getThirdPartyClasspath() {
        final List<String> classpath = new ArrayList<String>();

        final DefaultListModel listModel = (DefaultListModel) pathList.getModel();
        for (int i = 0; i < listModel.size(); ++i) {
            final String path = (String) listModel.get(i);
            classpath.add(path);
        }

        return classpath;
    }

    /**
     * Have the settings been modified?
     *
     * @return true if the settings have been modified.
     * @throws IOException if the properties cannot be read.
     */
    public boolean isModified() throws IOException {
        return haveLocationsChanged()
                || activeLocation.hasChangedFrom(locationModel.getActiveLocation())
                || !getThirdPartyClasspath().equals(thirdPartyClasspath)
                || testClassesCheckbox.isSelected() != scanTestClasses
                || scanNonJavaFilesCheckbox.isSelected() != scanNonJavaFiles
                || suppressErrorsCheckbox.isSelected() != suppressingErrors;
    }

    private boolean haveLocationsChanged() throws IOException {
        if (!ObjectUtils.equals(locations, locationModel.getLocations())) {
            return true;
        }

        for (int i = 0; i < locations.size(); ++i) {
            if (locations.get(i).hasChangedFrom(locationModel.getLocationAt(i))) {
                return true;
            }
        }
        return false;
    }

    public List<ConfigurationLocation> getConfigurationLocations() {
        return Collections.unmodifiableList(locationModel.getLocations());
    }

    public void setConfigurationLocations(final List<ConfigurationLocation> locations) {
        this.locations = locations;
        locationModel.setLocations(locations);
    }

    public void setActiveLocation(final ConfigurationLocation activeLocation) {
        this.activeLocation = activeLocation;
        locationModel.setActiveLocation(activeLocation);
    }

    public ConfigurationLocation getActiveLocation() {
        return locationModel.getActiveLocation();
    }

    public void setDefaultLocation(final ConfigurationLocation defaultLocation) {
        this.defaultLocation = defaultLocation;
    }

    /**
     * Process the addition of a configuration location.
     */
    protected final class AddLocationAction extends ToolbarAction {
        private static final long serialVersionUID = -7266120887003483814L;

        public AddLocationAction() {
            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);

            putValue(Action.NAME, resources.getString("config.file.add.text"));
            putValue(Action.SHORT_DESCRIPTION, resources.getString("config.file.add.tooltip"));
            putValue(Action.LONG_DESCRIPTION, resources.getString("config.file.add.tooltip"));
        }

        public void actionPerformed(final ActionEvent e) {
            final LocationDialogue dialogue = new LocationDialogue(project, getThirdPartyClasspath());

            dialogue.setVisible(true);

            if (dialogue.isCommitted()) {
                final ConfigurationLocation newLocation = dialogue.getConfigurationLocation();
                if (locationModel.getLocations().contains(newLocation)) {
                    final ResourceBundle resources = ResourceBundle.getBundle(
                            CheckStyleConstants.RESOURCE_BUNDLE);

                    Messages.showWarningDialog(project,
                            resources.getString("config.file.error.duplicate.text"),
                            resources.getString("config.file.error.duplicate.title"));

                } else {
                    locationModel.addLocation(dialogue.getConfigurationLocation());
                }
            }
        }
    }

    /**
     * Process the removal of a configuration location.
     */
    protected final class RemoveLocationAction extends ToolbarAction {
        private static final long serialVersionUID = -799542186049804472L;

        public RemoveLocationAction() {
            final ResourceBundle resources = ResourceBundle.getBundle(CheckStyleConstants.RESOURCE_BUNDLE);

            putValue(Action.NAME, resources.getString("config.file.remove.text"));
            putValue(Action.SHORT_DESCRIPTION, resources.getString("config.file.remove.tooltip"));
            putValue(Action.LONG_DESCRIPTION, resources.getString("config.file.remove.tooltip"));
        }

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
    protected final class EditPropertiesAction extends ToolbarAction {
        private static final long serialVersionUID = -799542186049804472L;

        public EditPropertiesAction() {
            final ResourceBundle resources = ResourceBundle.getBundle(CheckStyleConstants.RESOURCE_BUNDLE);

            putValue(Action.NAME, resources.getString("config.file.properties.text"));
            putValue(Action.SHORT_DESCRIPTION, resources.getString("config.file.properties.tooltip"));
            putValue(Action.LONG_DESCRIPTION, resources.getString("config.file.properties.tooltip"));
        }

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

    protected abstract class ToolbarAction extends AbstractAction implements AnActionButtonRunnable {
        private static final long serialVersionUID = 7091312536206510956L;

        @Override
        public void run(final AnActionButton anActionButton) {
            actionPerformed(null);
        }
    }

    /**
     * Process the addition of a path element.
     */
    protected final class AddPathAction extends ToolbarAction {
        private static final long serialVersionUID = -1389576037231727360L;

        /**
         * Create a new add path action.
         */
        public AddPathAction() {
            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);

            putValue(Action.NAME, resources.getString("config.path.add.text"));
            putValue(Action.SHORT_DESCRIPTION, resources.getString("config.path.add.tooltip"));
            putValue(Action.LONG_DESCRIPTION, resources.getString("config.path.add.tooltip"));
        }

        public void actionPerformed(final ActionEvent e) {
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new ExtensionFileFilter("jar"));
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fileChooser.setCurrentDirectory(getProjectPath());

            final int result = fileChooser.showOpenDialog(CheckStyleConfigPanel.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                ((DefaultListModel) pathList.getModel()).addElement(
                        fileChooser.getSelectedFile().getAbsolutePath());
            }
        }
    }

    private File getProjectPath() {
        final VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) {
            return null;
        }

        return new File(baseDir.getPath());
    }

    /**
     * Process the editing of a path element.
     */
    protected final class EditPathAction extends ToolbarAction {
        private static final long serialVersionUID = -1455378231580505750L;

        /**
         * Create a new edit path action.
         */
        public EditPathAction() {
            final ResourceBundle resources = ResourceBundle.getBundle(CheckStyleConstants.RESOURCE_BUNDLE);

            putValue(Action.NAME, resources.getString("config.path.edit.text"));
            putValue(Action.SHORT_DESCRIPTION, resources.getString("config.path.edit.tooltip"));
            putValue(Action.LONG_DESCRIPTION, resources.getString("config.path.edit.tooltip"));
        }

        public void actionPerformed(final ActionEvent e) {
            final int selected = pathList.getSelectedIndex();
            if (selected < 0) {
                return;
            }

            final DefaultListModel listModel = (DefaultListModel) pathList.getModel();
            final String selectedFile = (String) listModel.get(selected);

            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new ExtensionFileFilter("jar"));
            fileChooser.setSelectedFile(new File(selectedFile));
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

            final int result = fileChooser.showOpenDialog(CheckStyleConfigPanel.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                listModel.remove(selected);
                listModel.add(selected, fileChooser.getSelectedFile().getAbsolutePath());
                pathList.setSelectedIndex(selected);
            }
        }
    }

    /**
     * Process the removal of a path element.
     */
    protected final class RemovePathAction extends ToolbarAction {
        private static final long serialVersionUID = 7339136485307147623L;

        /**
         * Create a new add path action.
         */
        public RemovePathAction() {
            final ResourceBundle resources = ResourceBundle.getBundle(CheckStyleConstants.RESOURCE_BUNDLE);

            putValue(Action.NAME, resources.getString("config.path.remove.text"));
            putValue(Action.SHORT_DESCRIPTION, resources.getString("config.path.remove.tooltip"));
            putValue(Action.LONG_DESCRIPTION, resources.getString("config.path.remove.tooltip"));
        }

        public void actionPerformed(final ActionEvent e) {
            final int[] selected = pathList.getSelectedIndices();
            if (selected == null || selected.length == 0) {
                return;
            }

            for (final int index : selected) {
                ((DefaultListModel) pathList.getModel()).remove(index);
            }
        }
    }

    /**
     * Process the move up of a path element.
     */
    protected final class MoveUpPathAction extends ToolbarAction {
        private static final long serialVersionUID = -1230778908605654344L;

        /**
         * Create a new move-up path action.
         */
        public MoveUpPathAction() {
            final ResourceBundle resources = ResourceBundle.getBundle(CheckStyleConstants.RESOURCE_BUNDLE);

            putValue(Action.NAME, resources.getString("config.path.move-up.text"));
            putValue(Action.SHORT_DESCRIPTION, resources.getString("config.path.move-up.tooltip"));
            putValue(Action.LONG_DESCRIPTION, resources.getString("config.path.move-up.tooltip"));
        }

        public void actionPerformed(final ActionEvent e) {
            final int selected = pathList.getSelectedIndex();
            if (selected < 1) {
                return;
            }

            final DefaultListModel listModel = (DefaultListModel) pathList.getModel();
            final Object element = listModel.remove(selected);
            listModel.add(selected - 1, element);

            pathList.setSelectedIndex(selected - 1);
        }
    }

    /**
     * Process the move down of a path element.
     */
    protected final class MoveDownPathAction extends ToolbarAction {
        private static final long serialVersionUID = 1222511743014969175L;

        /**
         * Create a new move-down path action.
         */
        public MoveDownPathAction() {
            final ResourceBundle resources = ResourceBundle.getBundle(CheckStyleConstants.RESOURCE_BUNDLE);

            putValue(Action.NAME, resources.getString("config.path.move-down.text"));
            putValue(Action.SHORT_DESCRIPTION, resources.getString("config.path.move-down.tooltip"));
            putValue(Action.LONG_DESCRIPTION, resources.getString("config.path.move-down.tooltip"));
        }

        public void actionPerformed(final ActionEvent e) {
            final DefaultListModel listModel = (DefaultListModel)
                    pathList.getModel();
            final int selected = pathList.getSelectedIndex();
            if (selected == -1 || selected == (listModel.getSize() - 1)) {
                return;
            }

            final Object element = listModel.remove(selected);
            listModel.add(selected + 1, element);

            pathList.setSelectedIndex(selected + 1);
        }
    }

    private class DisableForDefaultUpdater implements AnActionButtonUpdater {
        @Override
        public boolean isEnabled(final AnActionEvent e) {
            final int selectedItem = locationTable.getSelectedRow();
            return selectedItem == -1 || !ObjectUtils.equals(locationModel.getLocationAt(selectedItem), defaultLocation);
        }
    }
}
