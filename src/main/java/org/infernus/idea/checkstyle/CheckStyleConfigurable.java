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

        boolean modified = oldConfig.hasChangedFrom(configurationFromPanel());
        if (LOG.isDebugEnabled()) {
            LOG.debug("Has config changed? " + modified);
        }
        return modified;
    }

    public void apply() {
        pluginConfigurationManager.setCurrent(configurationFromPanel(), true);

        configurationInvalidator.invalidateCachedResources();
    }

    /**
     * The pre-commit scan flag is also editable on the Version Control &gt; Commit page, which the settings
     * tree applies before us. Carrying the live value through when our own checkbox was left alone stops
     * us discarding an edit made there in the same session.
     */
    private PluginConfiguration configurationFromPanel() {
        final PluginConfigurationBuilder builder = PluginConfigurationBuilder
                .from(configPanel.getPluginConfiguration());
        if (!configPanel.isScanBeforeCheckinModified()) {
            builder.withScanBeforeCheckin(pluginConfigurationManager.getCurrent().isScanBeforeCheckin());
        }
        return builder.build();
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
