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
 * Allows setting of file properties.
 */
public class PropertiesDialogue extends DialogWrapper {

    private final PropertiesPanel propertiesPanel;


    public PropertiesDialogue(@Nullable final Dialog parent,
                              @NotNull final Project project,
                              @NotNull final CheckstyleProjectService checkstyleProjectService) {
        super(project, parent, false, IdeModalityType.PROJECT);

        this.propertiesPanel = new PropertiesPanel(project, checkstyleProjectService);

        setTitle(CheckStyleBundle.message("config.file.properties.title"));
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return propertiesPanel;
    }

    /**
     * Get the configuration location entered in the dialogue, or null if no valid location was entered.
     *
     * @return the location or null if no valid location entered.
     */
    public ConfigurationLocation getConfigurationLocation() {
        return propertiesPanel.getConfigurationLocation();
    }

    /**
     * Set the configuration location.
     *
     * @param configurationLocation the location.
     */
    public void setConfigurationLocation(final ConfigurationLocation configurationLocation) {
        propertiesPanel.setConfigurationLocation(configurationLocation);
    }
}
