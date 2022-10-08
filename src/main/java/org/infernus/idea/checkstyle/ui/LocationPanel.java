package org.infernus.idea.checkstyle.ui;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.util.ui.JBUI;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Paths;
import java.util.UUID;

import static org.infernus.idea.checkstyle.model.ConfigurationType.*;
import static org.infernus.idea.checkstyle.ui.LocationPanel.LocationType.*;
import static org.infernus.idea.checkstyle.util.Strings.isBlank;

public class LocationPanel extends JPanel {

    enum LocationType {
        FILE, HTTP, CLASSPATH
    }

    private static final Insets COMPONENT_INSETS = JBUI.insets(4);

    private final JButton browseButton = new JButton(new BrowseAction());
    private final JTextField fileLocationField = new JTextField(20);
    private final JTextField urlLocationField = new JTextField(20);
    private final JTextField classpathLocationField = new JTextField(20);
    private final JRadioButton fileLocationRadio = new JRadioButton();
    private final JRadioButton urlLocationRadio = new JRadioButton();
    private final JRadioButton classpathLocationRadio = new JRadioButton();
    private final JTextField descriptionField = new JTextField();
    private final ComboBox<NamedScope> scopeComboBox = new ComboBox<>();
    private final JCheckBox relativeFileCheckbox = new JCheckBox();
    private final JCheckBox insecureHttpCheckbox = new JCheckBox();
    private final JLabel classpathLocationReminderLabel = new JLabel();

    private final Project project;

    public LocationPanel(final Project project) {
        super(new GridBagLayout());

        if (project == null) {
            throw new IllegalArgumentException("Project may not be null");
        }
        this.project = project;

        initialise();
    }

    private void initialise() {
        relativeFileCheckbox.setText(CheckStyleBundle.message("config.file.relative-file.text"));
        relativeFileCheckbox.setToolTipText(CheckStyleBundle.message("config.file.relative-file.tooltip"));
        insecureHttpCheckbox.setText(CheckStyleBundle.message("config.file.insecure-http.text"));
        insecureHttpCheckbox.setToolTipText(CheckStyleBundle.message("config.file.insecure-http.tooltip"));

        fileLocationRadio.setText(CheckStyleBundle.message("config.file.file.text"));
        fileLocationRadio.addActionListener(new RadioButtonActionListener());
        urlLocationRadio.setText(CheckStyleBundle.message("config.file.url.text"));
        urlLocationRadio.addActionListener(new RadioButtonActionListener());
        classpathLocationRadio.setText(CheckStyleBundle.message("config.file.classpath.text"));
        classpathLocationRadio.addActionListener(new RadioButtonActionListener());
        classpathLocationReminderLabel.setText(CheckStyleBundle.message("config.file.classpath.reminder"));

        final ButtonGroup locationGroup = new ButtonGroup();
        locationGroup.add(fileLocationRadio);
        locationGroup.add(urlLocationRadio);
        locationGroup.add(classpathLocationRadio);

        fileLocationRadio.setSelected(true);
        enabledLocation(FILE);

        final JLabel descriptionLabel = new JLabel(CheckStyleBundle.message("config.file.description.text"));
        descriptionField.setToolTipText(CheckStyleBundle.message("config.file.description.tooltip"));

        final JLabel fileLocationLabel = new JLabel(CheckStyleBundle.message("config.file.file.label"));
        final JLabel urlLocationlabel = new JLabel(CheckStyleBundle.message("config.file.url.label"));
        final JLabel classpathLocationLabel = new JLabel(CheckStyleBundle.message("config.file.classpath.label"));

        final JLabel scopeLabel = new JLabel(CheckStyleBundle.message("config.file.scope.label"));
        NamedScopeHelper.getAllScopes(project).forEach(this.scopeComboBox::addItem);
        this.scopeComboBox.setSelectedItem(NamedScopeHelper.getDefaultScope(project));
        this.scopeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
                var presentableName = ((NamedScope) value).getPresentableName();
                super.getListCellRendererComponent(list, presentableName, index, isSelected, cellHasFocus);
                return this;
            }
        });

        setBorder(JBUI.Borders.empty(8, 8, 4, 8));

        add(descriptionLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
        add(descriptionField, new GridBagConstraints(1, 0, 2, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, COMPONENT_INSETS, 0, 0));

        add(fileLocationRadio, new GridBagConstraints(0, 1, 3, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));

        add(fileLocationLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
        add(fileLocationField, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, COMPONENT_INSETS, 0, 0));
        add(browseButton, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));

        add(relativeFileCheckbox, new GridBagConstraints(1, 3, 2, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));

        add(urlLocationRadio, new GridBagConstraints(0, 4, 3, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, JBUI.insets(8, 4, 4, 4), 0, 0));

        add(urlLocationlabel, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
        add(urlLocationField, new GridBagConstraints(1, 5, 2, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, COMPONENT_INSETS, 0, 0));

        add(insecureHttpCheckbox, new GridBagConstraints(1, 6, 2, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));

        add(classpathLocationRadio, new GridBagConstraints(0, 7, 3, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, JBUI.insets(8, 4, 4, 4), 0, 0));

        add(classpathLocationLabel, new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));
        add(classpathLocationField, new GridBagConstraints(1, 8, 2, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, COMPONENT_INSETS, 0, 0));
        add(classpathLocationReminderLabel, new GridBagConstraints(1, 9, 2, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, COMPONENT_INSETS, 0, 0));

        add(Box.createVerticalGlue(), new GridBagConstraints(0, 10, 3, 1, 0.0, 1.0,
                GridBagConstraints.WEST, GridBagConstraints.VERTICAL, COMPONENT_INSETS, 0, 0));


        add(scopeLabel, new GridBagConstraints(0, 12, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, COMPONENT_INSETS, 0, 0));

        add(scopeComboBox, new GridBagConstraints(1, 12, 2, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, COMPONENT_INSETS, 0, 0));
    }

    private void enabledLocation(final LocationType locationType) {
        fileLocationField.setEnabled(locationType == FILE);
        browseButton.setEnabled(locationType == FILE);
        relativeFileCheckbox.setEnabled(locationType == FILE);

        urlLocationField.setEnabled(locationType == HTTP);
        insecureHttpCheckbox.setEnabled(locationType == HTTP);

        classpathLocationField.setEnabled(locationType == CLASSPATH);
    }

    private ConfigurationType typeOfFile() {
        if (relativeFileCheckbox.isSelected()) {
            return PROJECT_RELATIVE;
        }
        return LOCAL_FILE;
    }

    private ConfigurationType typeOfUrl() {
        if (insecureHttpCheckbox.isSelected()) {
            return INSECURE_HTTP_URL;
        }
        return HTTP_URL;
    }

    /**
     * Get the configuration location entered in the dialogue, or null if no valid location was entered.
     *
     * @return the location or null if no valid location entered.
     */
    public ConfigurationLocation getConfigurationLocation() {
        final NamedScope namedScope = (NamedScope) scopeComboBox.getSelectedItem();
        final String newId = UUID.randomUUID().toString();

        if (fileLocationField.isEnabled()) {
            if (isNotBlank(fileLocation())) {
                return configurationLocationFactory().create(
                        project,
                        newId,
                        typeOfFile(),
                        fileLocation(),
                        descriptionField.getText(),
                        namedScope);
            }

        } else if (urlLocationField.isEnabled()) {
            if (isNotBlank(urlLocation())) {
                return configurationLocationFactory().create(
                        project,
                        newId,
                        typeOfUrl(),
                        urlLocation(),
                        descriptionField.getText(),
                        namedScope);
            }

        } else if (classpathLocationField.isEnabled()) {
            if (isNotBlank(classpathLocation())) {
                return configurationLocationFactory().create(
                        project,
                        newId,
                        PLUGIN_CLASSPATH,
                        classpathLocation(),
                        descriptionField.getText(),
                        namedScope);
            }
        }



        return null;
    }

    private String urlLocation() {
        return trim(urlLocationField.getText());
    }

    private String classpathLocation() {
        return trim(classpathLocationField.getText());
    }

    private String fileLocation() {
        final String filename = trim(fileLocationField.getText());

        if (new File(filename).exists()) {
            return filename;
        }

        final File projectRelativePath = projectRelativeFileOf(filename);
        if (projectRelativePath.exists()) {
            return projectRelativePath.getAbsolutePath();
        }

        return filename;
    }

    private File projectRelativeFileOf(final String filename) {
        return Paths.get(new File(project.getBasePath(), filename).getAbsolutePath())
                .normalize()
                .toAbsolutePath()
                .toFile();
    }

    private String trim(final String text) {
        if (text != null) {
            return text.trim();
        }
        return null;
    }

    private ConfigurationLocationFactory configurationLocationFactory() {
        return project.getService(ConfigurationLocationFactory.class);
    }

    /*
     * This is a port from commons-lang 2.4, in order to get around the absence of commons-lang in
     * some packages of IDEA 7.x.
     */
    private boolean isNotBlank(final String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return false;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }


    /**
     * Set the configuration location.
     *
     * @param configurationLocation the location.
     */
    public void setConfigurationLocation(final ConfigurationLocation configurationLocation) {
        relativeFileCheckbox.setSelected(false);
        insecureHttpCheckbox.setSelected(false);

        if (configurationLocation == null) {
            fileLocationRadio.setEnabled(true);
            fileLocationField.setText(null);

        } else if (configurationLocation.getType() == LOCAL_FILE
                || configurationLocation.getType() == PROJECT_RELATIVE) {
            fileLocationRadio.setEnabled(true);
            fileLocationField.setText(configurationLocation.getLocation());
            relativeFileCheckbox.setSelected(configurationLocation.getType() == PROJECT_RELATIVE);

        } else if (configurationLocation.getType() == HTTP_URL
                || configurationLocation.getType() == INSECURE_HTTP_URL) {
            urlLocationRadio.setEnabled(true);
            urlLocationField.setText(configurationLocation.getLocation());
            insecureHttpCheckbox.setSelected(configurationLocation.getType() == INSECURE_HTTP_URL);

        } else if (configurationLocation.getType() == PLUGIN_CLASSPATH) {
            classpathLocationField.setEnabled(true);
            classpathLocationField.setText(configurationLocation.getLocation());

        } else {
            throw new IllegalArgumentException("Unsupported configuration type: " + configurationLocation.getType());
        }
    }

    private final class BrowseAction extends AbstractAction {
        private static final long serialVersionUID = -992858528081327052L;

        BrowseAction() {
            putValue(Action.NAME, CheckStyleBundle.message(
                    "config.file.browse.text"));
            putValue(Action.SHORT_DESCRIPTION,
                    CheckStyleBundle.message("config.file.browse.tooltip"));
            putValue(Action.LONG_DESCRIPTION,
                    CheckStyleBundle.message("config.file.browse.tooltip"));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            final VirtualFile toSelect;
            final String configFilePath = fileLocation();
            if (!isBlank(configFilePath)) {
                toSelect = LocalFileSystem.getInstance().findFileByPath(configFilePath);
            } else {
                toSelect = ProjectUtil.guessProjectDir(project);
            }

            final FileChooserDescriptor descriptor = new ExtensionFileChooserDescriptor(
                    (String) getValue(Action.NAME),
                    (String) getValue(Action.SHORT_DESCRIPTION),
                    true, "xml", "checkstyle");
            final VirtualFile chosen = FileChooser.chooseFile(descriptor, LocationPanel.this, project, toSelect);
            if (chosen != null) {
                final File newConfigFile = VfsUtilCore.virtualToIoFile(chosen);
                fileLocationField.setText(newConfigFile.getAbsolutePath());
            }
        }
    }

    /**
     * Handles radio button selections.
     */
    private final class RadioButtonActionListener implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent e) {
            if (urlLocationRadio.isSelected()) {
                enabledLocation(HTTP);

            } else if (fileLocationRadio.isSelected()) {
                enabledLocation(FILE);

            } else if (classpathLocationRadio.isSelected()) {
                enabledLocation(CLASSPATH);

            } else {
                throw new IllegalStateException("Unknown radio button state");
            }
        }
    }
}
