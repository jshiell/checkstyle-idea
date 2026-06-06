package org.infernus.idea.checkstyle.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Allows editing of a configuration location's description, path, and properties.
 */
public class PropertiesDialogue extends DialogWrapper {

    private final LocationPanel locationPanel;
    private final PropertiesPanel propertiesPanel;


    public PropertiesDialogue(@Nullable final Dialog parent,
                              @NotNull final Project project,
                              @NotNull final CheckstyleProjectService checkstyleProjectService) {
        super(project, parent, false, IdeModalityType.IDE);

        this.locationPanel = new LocationPanel(project);
        this.propertiesPanel = new PropertiesPanel(project, checkstyleProjectService);

        setTitle(CheckStyleBundle.message("config.file.edit.title"));
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(locationPanel, BorderLayout.NORTH);
        panel.add(propertiesPanel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Get the configuration location entered in the dialogue, or null if no valid location was entered.
     *
     * @return the location or null if no valid location entered.
     */
    public ConfigurationLocation getConfigurationLocation() {
        final ConfigurationLocation location = propertiesPanel.getConfigurationLocation();
        locationPanel.applyChangesTo(location);
        return location;
    }

    /**
     * Set the configuration location.
     *
     * @param configurationLocation the location.
     */
    public void setConfigurationLocation(final ConfigurationLocation configurationLocation) {
        locationPanel.setConfigurationLocation(configurationLocation);
        locationPanel.setTypeSelectionEnabled(false);
        propertiesPanel.setConfigurationLocation(configurationLocation);
    }
}
