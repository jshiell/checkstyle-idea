package org.infernus.idea.checkstyle.ui;

import com.intellij.openapi.ui.Messages;
import com.intellij.util.ObjectUtils;
import org.infernus.idea.checkstyle.CheckStyleConstants;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Provides an input box and browse button for CheckStyle file selection.
 *
 * @author James Shiell
 * @version 1.0
 */
public final class CheckStyleConfigPanel extends JPanel {

    private final JList pathList = new JList(new DefaultListModel());
    private final JButton editPathButton = new JButton(new EditPathAction());
    private final JButton removePathButton = new JButton(new RemovePathAction());
    private final JButton moveUpPathButton = new JButton(new MoveUpPathAction());
    private final JButton moveDownPathButton = new JButton(new MoveDownPathAction());

    private final JCheckBox testClassesCheckbox = new JCheckBox();

    private final LocationTableModel locationModel = new LocationTableModel();
    private final JTable locationTable = new JTable(locationModel);
    private final JButton addLocationButton = new JButton(new AddLocationAction());
    private final JButton removeLocationButton = new JButton(new RemoveLocationAction());
    private final JButton editLocationPropertiesButton = new JButton(new EditPropertiesAction());

    private boolean scanTestClasses;
    private List<String> thirdPartyClasspath;
    private List<ConfigurationLocation> locations;
    private ConfigurationLocation activeLocation;
    private ConfigurationLocation defaultLocation;

    /**
     * Plug-in reference.
     */
    private CheckStylePlugin plugin;

    /**
     * Create a new panel.
     *
     * @param plugin the plugin that owns this panel.
     */
    public CheckStyleConfigPanel(final CheckStylePlugin plugin) {
        super(new BorderLayout());

        if (plugin == null) {
            throw new IllegalArgumentException("Plugin may not be null.");
        }

        this.plugin = plugin;

        initialise();
    }

    /**
     * Initialise the view.
     */
    protected void initialise() {
        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);

        final JPanel configFilePanel = buildConfigPanel();

        final JPanel pathPanel = buildPathPanel();

        final JTabbedPane rootTabPane = new JTabbedPane();
        rootTabPane.add(configFilePanel, resources.getString("config.file.tab"));
        rootTabPane.add(pathPanel, resources.getString("config.path.tab"));

        add(rootTabPane, BorderLayout.CENTER);
    }

    private JPanel buildConfigPanel() {
        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);

        testClassesCheckbox.setText(resources.getString(
                "config.test-classes.checkbox.text"));
        testClassesCheckbox.setToolTipText(resources.getString(
                "config.test-classes.checkbox.tooltip"));

        editLocationPropertiesButton.setEnabled(false);
        removeLocationButton.setEnabled(false);

        final JToolBar locationToolBar = new JToolBar();
        locationToolBar.setFloatable(false);
        locationToolBar.add(addLocationButton);
        locationToolBar.add(editLocationPropertiesButton);
        locationToolBar.add(removeLocationButton);

        locationTable.getSelectionModel().addListSelectionListener(new LocationTableSelectionListener());
        final JScrollPane locationScrollPane = new JScrollPane(locationTable,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        final JPanel locationPanel = new JPanel(new BorderLayout());
        locationPanel.add(locationToolBar, BorderLayout.NORTH);
        locationPanel.add(locationScrollPane, BorderLayout.CENTER);

        final JPanel configFilePanel = new JPanel(new GridBagLayout());
        configFilePanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        configFilePanel.setOpaque(false);

        configFilePanel.add(testClassesCheckbox, new GridBagConstraints(
                0, 0, 3, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
        configFilePanel.add(locationPanel, new GridBagConstraints(
                0, 1, 3, 1, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0));

        return configFilePanel;
    }

    private JPanel buildPathPanel() {
        final JButton addPathButton = new JButton(new AddPathAction());

        editPathButton.setEnabled(false);
        removePathButton.setEnabled(false);
        moveUpPathButton.setEnabled(false);
        moveDownPathButton.setEnabled(false);

        pathList.addListSelectionListener(new PathListSelectionListener());
        final JScrollPane pathListScroll = new JScrollPane(pathList);
        pathListScroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        pathListScroll.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        final JPanel pathPanel = new JPanel(new GridBagLayout());
        pathPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        pathPanel.setOpaque(false);
        pathPanel.add(pathListScroll, new GridBagConstraints(
                0, 0, 1, 7, 1.0, 1.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0));
        pathPanel.add(addPathButton, new GridBagConstraints(
                1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
        pathPanel.add(editPathButton, new GridBagConstraints(
                1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
        pathPanel.add(removePathButton, new GridBagConstraints(
                1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
        pathPanel.add(new JSeparator(), new GridBagConstraints(
                1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
        pathPanel.add(moveUpPathButton, new GridBagConstraints(
                1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
        pathPanel.add(moveDownPathButton, new GridBagConstraints(
                1, 5, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
        pathPanel.add(Box.createVerticalGlue(), new GridBagConstraints(
                1, 6, 1, 1, 0.0, 1.0, GridBagConstraints.NORTH,
                GridBagConstraints.VERTICAL, new Insets(4, 4, 4, 4), 0, 0));
        return pathPanel;
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
        this.scanTestClasses = testClassesCheckbox.isSelected();
        return scanTestClasses;
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

        final DefaultListModel listModel = (DefaultListModel)
                pathList.getModel();
        listModel.clear();

        for (final String classPathFile : thirdPartyClasspath) {
            listModel.addElement(classPathFile);
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

        final DefaultListModel listModel = (DefaultListModel)
                pathList.getModel();
        for (int i = 0; i < listModel.size(); ++i) {
            final String path = (String) listModel.get(i);
            classpath.add(path);
        }

        return classpath;
    }

    /**
     * Have the settings been modified?
     *
     * @return true if the settngs have been modified.
     */
    public boolean isModified() {
        return !ObjectUtils.equals(locations, locationModel.getLocations())
                || !ObjectUtils.equals(activeLocation, locationModel.getActiveLocation())
                || !getThirdPartyClasspath().equals(thirdPartyClasspath)
                || testClassesCheckbox.isSelected() != scanTestClasses;
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
    protected final class AddLocationAction extends AbstractAction {
        private static final long serialVersionUID = -7266120887003483814L;

        public AddLocationAction() {
            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);

            putValue(Action.NAME, resources.getString("config.file.add.text"));
            putValue(Action.SHORT_DESCRIPTION,
                    resources.getString("config.file.add.tooltip"));
            putValue(Action.LONG_DESCRIPTION,
                    resources.getString("config.file.add.tooltip"));
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(final ActionEvent e) {
            final LocationDialogue dialogue = new LocationDialogue(plugin.getProject());

            dialogue.setVisible(true);

            if (dialogue.isCommitted()) {
                final ConfigurationLocation newLocation = dialogue.getConfigurationLocation();
                if (locationModel.getLocations().contains(newLocation)) {
                    final ResourceBundle resources = ResourceBundle.getBundle(
                            CheckStyleConstants.RESOURCE_BUNDLE);

                    Messages.showWarningDialog(plugin.getProject(),
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
    protected final class RemoveLocationAction extends AbstractAction {
        private static final long serialVersionUID = -799542186049804472L;

        public RemoveLocationAction() {
            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);

            putValue(Action.NAME, resources.getString("config.file.remove.text"));
            putValue(Action.SHORT_DESCRIPTION,
                    resources.getString("config.file.remove.tooltip"));
            putValue(Action.LONG_DESCRIPTION,
                    resources.getString("config.file.remove.tooltip"));
        }

        /**
         * {@inheritDoc}
         */
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
    protected final class EditPropertiesAction extends AbstractAction {
        private static final long serialVersionUID = -799542186049804472L;

        public EditPropertiesAction() {
            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);

            putValue(Action.NAME, resources.getString("config.file.properties.text"));
            putValue(Action.SHORT_DESCRIPTION,
                    resources.getString("config.file.properties.tooltip"));
            putValue(Action.LONG_DESCRIPTION,
                    resources.getString("config.file.properties.tooltip"));
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(final ActionEvent e) {
            final int selectedIndex = locationTable.getSelectedRow();
            if (selectedIndex == -1) {
                return;
            }

            final ConfigurationLocation location = locationModel.getLocationAt(selectedIndex);

            final PropertiesDialogue propertiesDialogue = new PropertiesDialogue(plugin.getProject());
            propertiesDialogue.setConfigurationLocation(location);

            propertiesDialogue.setVisible(true);

            if (propertiesDialogue.isCommitted()) {
                final ConfigurationLocation editedLocation = propertiesDialogue.getConfigurationLocation();
                locationModel.updateLocation(location, editedLocation);
            }
        }
    }

    /**
     * Location table selection listener.
     */
    protected final class LocationTableSelectionListener
            implements ListSelectionListener {
        /**
         * {@inheritDoc}
         */
        public void valueChanged(final ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) {
                return;
            }

            final int selectedItem = locationTable.getSelectedRow();
            if (selectedItem == -1) {
                editLocationPropertiesButton.setEnabled(false);
                removeLocationButton.setEnabled(false);

            } else {
                final ConfigurationLocation location = locationModel.getLocationAt(selectedItem);

                editLocationPropertiesButton.setEnabled(!ObjectUtils.equals(location, defaultLocation));
                removeLocationButton.setEnabled(!ObjectUtils.equals(location, defaultLocation));
            }
        }
    }

    /**
     * Path list selection listener.
     */
    protected final class PathListSelectionListener
            implements ListSelectionListener {
        /**
         * {@inheritDoc}
         */
        public void valueChanged(final ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) {
                return;
            }

            final int[] selectedItems = pathList.getSelectedIndices();
            final boolean single = selectedItems != null
                    && selectedItems.length == 1;
            final boolean multiple = selectedItems != null
                    && selectedItems.length > 1;

            final DefaultListModel listModel = (DefaultListModel)
                    pathList.getModel();

            editPathButton.setEnabled(single);
            removePathButton.setEnabled(single || multiple);
            moveUpPathButton.setEnabled(single && pathList.getSelectedIndex()
                    != 0);
            moveDownPathButton.setEnabled(single && pathList.getSelectedIndex()
                    != (listModel.getSize() - 1));
        }
    }

    /**
     * Process the addition of a path element.
     */
    protected final class AddPathAction extends AbstractAction {

        /**
         * Create a new add path action.
         */
        public AddPathAction() {
            super();

            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);

            putValue(Action.NAME, resources.getString("config.path.add.text"));
            putValue(Action.SHORT_DESCRIPTION,
                    resources.getString("config.path.add.tooltip"));
            putValue(Action.LONG_DESCRIPTION,
                    resources.getString("config.path.add.tooltip"));
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(final ActionEvent e) {
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new ExtensionFileFilter("jar"));
            fileChooser.setFileSelectionMode(
                    JFileChooser.FILES_AND_DIRECTORIES);

            final int result = fileChooser.showOpenDialog(
                    CheckStyleConfigPanel.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                ((DefaultListModel) pathList.getModel()).addElement(
                        fileChooser.getSelectedFile().getAbsolutePath());
            }
        }
    }

    /**
     * Process the editing of a path element.
     */
    protected final class EditPathAction extends AbstractAction {

        /**
         * Create a new edit path action.
         */
        public EditPathAction() {
            super();

            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);

            putValue(Action.NAME, resources.getString("config.path.edit.text"));
            putValue(Action.SHORT_DESCRIPTION,
                    resources.getString("config.path.edit.tooltip"));
            putValue(Action.LONG_DESCRIPTION,
                    resources.getString("config.path.edit.tooltip"));
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(final ActionEvent e) {
            final int selected = pathList.getSelectedIndex();
            if (selected < 1) {
                return;
            }

            final DefaultListModel listModel = (DefaultListModel)
                    pathList.getModel();
            final String selectedFile = (String) listModel.get(selected);

            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new ExtensionFileFilter("jar"));
            fileChooser.setSelectedFile(new File(selectedFile));
            fileChooser.setFileSelectionMode(
                    JFileChooser.FILES_AND_DIRECTORIES);

            final int result = fileChooser.showOpenDialog(
                    CheckStyleConfigPanel.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                listModel.remove(selected);
                listModel.add(selected,
                        fileChooser.getSelectedFile().getAbsolutePath());
                pathList.setSelectedIndex(selected);
            }
        }
    }

    /**
     * Process the removal of a path element.
     */
    protected final class RemovePathAction extends AbstractAction {

        /**
         * Create a new add path action.
         */
        public RemovePathAction() {
            super();

            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);

            putValue(Action.NAME, resources.getString(
                    "config.path.remove.text"));
            putValue(Action.SHORT_DESCRIPTION,
                    resources.getString("config.path.remove.tooltip"));
            putValue(Action.LONG_DESCRIPTION,
                    resources.getString("config.path.remove.tooltip"));
        }

        /**
         * {@inheritDoc}
         */
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
    protected final class MoveUpPathAction extends AbstractAction {

        /**
         * Create a new move-up path action.
         */
        public MoveUpPathAction() {
            super();

            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);

            putValue(Action.NAME, resources.getString(
                    "config.path.move-up.text"));
            putValue(Action.SHORT_DESCRIPTION,
                    resources.getString("config.path.move-up.tooltip"));
            putValue(Action.LONG_DESCRIPTION,
                    resources.getString("config.path.move-up.tooltip"));
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(final ActionEvent e) {
            final int selected = pathList.getSelectedIndex();
            if (selected < 1) {
                return;
            }

            final DefaultListModel listModel = (DefaultListModel)
                    pathList.getModel();
            final Object element = listModel.remove(selected);
            listModel.add(selected - 1, element);

            pathList.setSelectedIndex(selected - 1);
        }
    }

    /**
     * Process a click on the browse button.
     */
    /**
     * Process the move down of a path element.
     */
    protected final class MoveDownPathAction extends AbstractAction {

        /**
         * Create a new move-down path action.
         */
        public MoveDownPathAction() {
            super();

            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);

            putValue(Action.NAME, resources.getString(
                    "config.path.move-down.text"));
            putValue(Action.SHORT_DESCRIPTION,
                    resources.getString("config.path.move-down.tooltip"));
            putValue(Action.LONG_DESCRIPTION,
                    resources.getString("config.path.move-down.tooltip"));
        }

        /**
         * {@inheritDoc}
         */
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
}
