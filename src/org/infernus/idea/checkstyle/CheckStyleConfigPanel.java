package org.infernus.idea.checkstyle;

import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Provides an input box and browse button for CheckStyle file selection.
 *
 * @author James Shiell
 * @version 1.0
 */
public final class CheckStyleConfigPanel extends JPanel {

    private final JLabel fileLabel = new JLabel();
    private final JTextField fileField = new JTextField();
    private final JButton browseButton = new JButton();
    private final JRadioButton useDefaultButton = new JRadioButton(
            new ConfigurationSelectionAction());
    private final JRadioButton useCustomButton = new JRadioButton(
            new ConfigurationSelectionAction());
    private final JList pathList = new JList(new DefaultListModel());
    private final JButton editPathButton = new JButton(new EditPathAction());
    private final JButton removePathButton = new JButton(
            new RemovePathAction());
    private final JButton moveUpPathButton = new JButton(
            new MoveUpPathAction());
    private final JButton moveDownPathButton = new JButton(
            new MoveDownPathAction());

    /**
     * Original configuration file for modification tests.
     */
    private String configFile;

    /**
     * Original third party classpath for modification tests.
     */
    private List<String> thirdPartyClasspath;

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

        useDefaultButton.setText(resources.getString(
                "config.file.default-radio.use-default.text"));
        useDefaultButton.setToolTipText(resources.getString(
                "config.file.default-radio.use-default.tooltip"));

        useCustomButton.setText(resources.getString(
                "config.file.default-radio.use-custom.text"));
        useCustomButton.setToolTipText(resources.getString(
                "config.file.default-radio.use-custom.tooltip"));

        final ButtonGroup configButtonGroup = new ButtonGroup();
        configButtonGroup.add(useDefaultButton);
        configButtonGroup.add(useCustomButton);
        configButtonGroup.setSelected(useDefaultButton.getModel(), true);

        fileField.setToolTipText(resources.getString(
                "config.file.label.tooltip"));
        fileField.setEditable(false);

        fileLabel.setText(resources.getString("config.file.label.text"));
        fileLabel.setToolTipText(resources.getString(
                "config.file.label.tooltip"));

        browseButton.setAction(new BrowseAction());

        final JPanel configFilePanel = new JPanel(new GridBagLayout());
        configFilePanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        configFilePanel.setOpaque(false);
        configFilePanel.add(useDefaultButton, new GridBagConstraints(
                0, 0, 3, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
        configFilePanel.add(useCustomButton, new GridBagConstraints(
                0, 1, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, 
                GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
        configFilePanel.add(fileLabel, new GridBagConstraints(
                0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(4, 20, 4, 4), 0, 0));
        configFilePanel.add(fileField, new GridBagConstraints(
                1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
        configFilePanel.add(browseButton, new GridBagConstraints(
                2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0));
        configFilePanel.add(Box.createVerticalGlue(), new GridBagConstraints(
                0, 3, 3, 1, 0.0, 1.0, GridBagConstraints.NORTH,
                GridBagConstraints.VERTICAL, new Insets(4, 4, 4, 4), 0, 0));

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

        final JTabbedPane rootTabPane = new JTabbedPane();
        rootTabPane.add(configFilePanel, resources.getString(
                "config.file.tab"));
        rootTabPane.add(pathPanel, resources.getString("config.path.tab"));

        add(rootTabPane, BorderLayout.CENTER);
    }

    /**
     * Get the currently selected configuration file.
     * <p/>
     * This method will only return the configuration file
     * if it currently exists.
     *
     * @return the currently selected configuration file.
     */
    public String getConfigFile() {
        if (useDefaultButton.isSelected()) {
            return null;
        }

        final String fileName = fileField.getText();

        this.configFile = fileName;
        if (fileName != null) {
            final File newConfigFile = new File(fileName);
            if (newConfigFile.exists()) {
                final String filePath = newConfigFile.getAbsolutePath();
                return plugin.tokenisePath(filePath);
            }
        }
        return null;
    }

    /**
     * Set the configuration file.
     *
     * @param configFile the configuration file.
     */
    public void setConfigFile(final File configFile) {
        if (configFile == null) {
            setConfigFile((String) null);
            useDefaultButton.setSelected(true);

        } else {
            setConfigFile(configFile.getAbsolutePath());
            useCustomButton.setSelected(true);
        }
    }

    /**
     * Set the configuration file.
     *
     * @param configFile the configuration file.
     */
    public void setConfigFile(final String configFile) {
        if (configFile == null) {
            fileField.setText("");
            useDefaultButton.setSelected(true);

        } else {
            fileField.setText(plugin.untokenisePath(configFile));
            useCustomButton.setSelected(true);
        }

        this.configFile = configFile;
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
            final String processedPath = plugin.untokenisePath(classPathFile);
            listModel.addElement(processedPath);
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
            classpath.add(plugin.tokenisePath(path));
        }
        
        return classpath;
    }

    /**
     * Reset the configuration to the original values.
     */
    public void reset() {
        setConfigFile(configFile);
        setThirdPartyClasspath(thirdPartyClasspath);
    }

    /**
     * Have the settings been modified?
     *
     * @return true if the settngs have been modified.
     */
    public boolean isModified() {
        return !ObjectUtils.equals(configFile, fileField.getText())
                || !getThirdPartyClasspath().equals(thirdPartyClasspath);
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
     * Process a click on the configuration file radio buttons.
     */
    protected final class ConfigurationSelectionAction extends AbstractAction {

        /**
         * Create a new configuration selection action.
         */
        public ConfigurationSelectionAction() {
            super();

            putValue(Action.NAME, "ConfigurationSelectionAction");
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(final ActionEvent e) {
            final boolean useDefault = useDefaultButton.isSelected();

            fileLabel.setEnabled(!useDefault);
            fileField.setEnabled(!useDefault);
            browseButton.setEnabled(!useDefault);
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

    /**
     * Process a click on the browse button.
     */
    protected final class BrowseAction extends AbstractAction {

        /**
         * Create a new browse action.
         */
        public BrowseAction() {
            super();

            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);
                         
            putValue(Action.NAME, resources.getString(
                    "config.file.browse.text"));
            putValue(Action.SHORT_DESCRIPTION,
                    resources.getString("config.file.browse.tooltip"));
            putValue(Action.LONG_DESCRIPTION,
                    resources.getString("config.file.browse.tooltip"));
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(final ActionEvent e) {
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new ExtensionFileFilter("xml"));

            final String configFilePath = getConfigFile();
            if (configFilePath != null) {
                fileChooser.setSelectedFile(new File(configFilePath));
            }

            final int result = fileChooser.showOpenDialog(
                    CheckStyleConfigPanel.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                fileField.setText(
                        fileChooser.getSelectedFile().getAbsolutePath());
            }
        }
    }

    /**
     * File filter for files with a specified extension.
     */
    protected final class ExtensionFileFilter extends FileFilter {

        private final String extension;

        /**
         * Create a filter for the given extension.
         *
         * @param extension the extension.
         */
        public ExtensionFileFilter(@NotNull final String extension) {
            this.extension = extension;
        }

        /**
         * {@inheritDoc}
         */
        public boolean accept(final File f) {
            if (f.isDirectory()) {
                return true;
            }

            final String fileName = f.getName();
            return fileName.toLowerCase().endsWith("." + extension);
        }

        /**
         * {@inheritDoc}
         */
        public String getDescription() {
            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);
            return resources.getString("config.file." + extension
                    + ".description");
        }
    }

}
