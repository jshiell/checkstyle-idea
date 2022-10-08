package org.infernus.idea.checkstyle;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleConfigurationEditor;
import org.infernus.idea.checkstyle.config.ModuleConfigurationState;
import org.infernus.idea.checkstyle.config.PluginConfiguration;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.ui.CheckStyleModuleConfigPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.util.*;
import java.util.stream.Collectors;

public class CheckStyleModuleConfigurationEditor implements ModuleConfigurationEditor {

    private final Module module;

    public CheckStyleModuleConfigurationEditor(@NotNull final Module module) {
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

    public void apply() {
        if (configPanel == null) {
            return;
        }

        final ModuleConfigurationState configuration = moduleConfigurationState();

        final ConfigurationLocation activeLocation = configPanel.getActiveLocation();
        final SortedSet<String> activeLocationIds = new TreeSet<>();
        if (activeLocation != null) {
            activeLocationIds.add(activeLocation.getId());
        }
        configuration.setActiveLocationIds(activeLocationIds);
        configuration.setExcluded(configPanel.isExcluded());

        reset();
    }

    private ModuleConfigurationState moduleConfigurationState() {
        return module.getService(ModuleConfigurationState.class);
    }

    private PluginConfigurationManager pluginConfigurationManager() {
        return module.getProject().getService(PluginConfigurationManager.class);
    }

    public void reset() {
        if (configPanel == null) {
            return;
        }

        final ModuleConfigurationState moduleConfiguration = moduleConfigurationState();
        PluginConfiguration pluginConfiguration = pluginConfigurationManager().getCurrent();

        configPanel.setConfigurationLocations(new ArrayList<>(pluginConfiguration.getLocations()));

        if (moduleConfiguration.isExcluded()) {
            configPanel.setExcluded(true);
        } else if (moduleConfiguration.isUsingModuleConfiguration()) {
            configPanel.setActiveLocations(moduleConfiguration.getActiveLocationIds().stream()
                    .map(id -> pluginConfiguration.getLocationById(id).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
        } else {
            configPanel.setActiveLocations(Collections.emptyList());
        }
    }

    public void disposeUIResources() {
        configPanel = null;
    }
}
