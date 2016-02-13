package org.infernus.idea.checkstyle;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.infernus.idea.checkstyle.ui.CheckStyleModuleConfigPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CheckStyleModuleConfigurable implements Configurable {

    private final Module module;

    public CheckStyleModuleConfigurable(@NotNull final Module module) {
        this.module = module;
    }

    private CheckStyleModuleConfigPanel configPanel;

    public String getDisplayName() {
        return CheckStyleBundle.message("plugin.configuration-name");
    }

    public String getHelpTopic() {
        return null;
    }

    public JComponent createComponent() {
        if (configPanel == null) {
            configPanel = new CheckStyleModuleConfigPanel();
        }

        reset();

        return configPanel;
    }

    public boolean isModified() {
        return configPanel != null && configPanel.isModified();
    }

    public void apply() throws ConfigurationException {
        if (configPanel == null) {
            return;
        }

        final CheckStyleModuleConfiguration configuration = getConfiguration();
        configuration.setActiveConfiguration(configPanel.getActiveLocation());
        configuration.setExcluded(configPanel.isExcluded());

        reset();
    }

    private CheckStyleModuleConfiguration getConfiguration() {
        return ModuleServiceManager.getService(module, CheckStyleModuleConfiguration.class);
    }

    public void reset() {
        if (configPanel == null) {
            return;
        }

        final CheckStyleModuleConfiguration configuration = getConfiguration();

        configPanel.setConfigurationLocations(configuration.getAndResolveConfigurationLocations());

        if (configuration.isExcluded()) {
            configPanel.setExcluded(true);
        } else if (configuration.isUsingModuleConfiguration()) {
            configPanel.setActiveLocation(configuration.getActiveConfiguration());
        } else {
            configPanel.setActiveLocation(null);
        }
    }

    public void disposeUIResources() {
        configPanel = null;
    }
}
