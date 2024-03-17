package org.infernus.idea.checkstyle;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.config.PluginConfiguration;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.ui.CheckStyleConfigPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;


/**
 * The "configurable component" required by IntelliJ IDEA to provide a Swing form for inclusion into the 'Settings'
 * dialog. Registered in {@code plugin.xml} as a {@code projectConfigurable} extension.
 */
public class CheckStyleConfigurable implements Configurable {
    private static final Logger LOG = Logger.getInstance(CheckStyleConfigurable.class);

    private final CheckStyleConfigPanel configPanel;
    private final PluginConfigurationManager pluginConfigurationManager;
    private final ConfigurationInvalidator configurationInvalidator;

    CheckStyleConfigurable(@NotNull final Project project) {
        this.pluginConfigurationManager = project.getService(PluginConfigurationManager.class);
        this.configurationInvalidator = project.getService(ConfigurationInvalidator.class);

        this.configPanel = new CheckStyleConfigPanel(project);
    }

    public String getDisplayName() {
        return CheckStyleBundle.message("plugin.configuration-name");
    }

    public String getHelpTopic() {
        return null;
    }

    public JComponent createComponent() {
        reset();
        return configPanel;
    }

    @Override
    public boolean isModified() {
        final PluginConfiguration oldConfig = pluginConfigurationManager.getCurrent();
        final PluginConfiguration newConfig = PluginConfigurationBuilder
                .from(configPanel.getPluginConfiguration())
                .withScanBeforeCheckin(oldConfig.isScanBeforeCheckin())
                .build();

        boolean result = !oldConfig.hasChangedFrom(newConfig);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Has config changed? " + result);
        }
        return result;
    }

    public void apply() {
        final PluginConfiguration newConfig = PluginConfigurationBuilder
                .from(configPanel.getPluginConfiguration())
                .withScanBeforeCheckin(pluginConfigurationManager.getCurrent().isScanBeforeCheckin())
                .build();
        pluginConfigurationManager.setCurrent(newConfig, true);

        configurationInvalidator.invalidateCachedResources();
    }

    public void reset() {
        final PluginConfiguration pluginConfig = pluginConfigurationManager.getCurrent();
        configPanel.showPluginConfiguration(pluginConfig);

        configurationInvalidator.invalidateCachedResources();
    }

    public void disposeUIResources() {
        // do nothing
    }
}
