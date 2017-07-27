package org.infernus.idea.checkstyle;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.checker.CheckerFactoryCache;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.ui.CheckStyleConfigPanel;
import org.infernus.idea.checkstyle.util.Notifications;
import org.infernus.idea.checkstyle.util.Objects;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * The "configurable component" required by IntelliJ IDEA to provide a Swing form for inclusion into the 'Settings'
 * dialog. Registered in {@code plugin.xml} as a {@code projectConfigurable} extension.
 */
public class CheckStyleConfigurable
        implements Configurable
{
    private static final Log LOG = LogFactory.getLog(CheckStyleConfigurable.class);

    private final Project project;

    private final CheckStyleConfigPanel configPanel;


    public CheckStyleConfigurable(@NotNull final Project project) {
        this(project, new CheckStyleConfigPanel(project));
    }

    CheckStyleConfigurable(@NotNull final Project project, @NotNull final CheckStyleConfigPanel configPanel) {
        this.project = project;
        this.configPanel = configPanel;
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
        LOG.trace("isModified() - enter");
        final CheckStyleConfiguration configuration = getConfiguration();
        final PluginConfigDto oldConfig = configuration.getCurrentPluginConfig();
        final PluginConfigDto newConfig = new PluginConfigDto(
                configPanel.getPluginConfiguration(), oldConfig.isScanBeforeCheckin());

        boolean result = !oldConfig.hasChangedFrom(newConfig);
        if (LOG.isTraceEnabled()) {
            LOG.trace("isModified() - exit - result=" + result);
        }
        return result;
    }


    public void apply() throws ConfigurationException {
        LOG.trace("apply() - enter");

        final CheckStyleConfiguration configuration = getConfiguration();
        final PluginConfigDto newConfig = new PluginConfigDto(configPanel.getPluginConfiguration(),
                configuration.getCurrentPluginConfig().isScanBeforeCheckin());
        configuration.setCurrentPluginConfig(newConfig, true);

        activateCurrentCheckstyleVersion(newConfig.getCheckstyleVersion(), newConfig.getThirdPartyClasspath());

        LOG.trace("apply() - exit");
    }

    private void activateCurrentCheckstyleVersion(final String checkstyleVersion, final List<String>
            thirdPartyClasspath) {
        // Invalidate cache *before* activating the new Checkstyle version
        getCheckerFactoryCache().invalidate();

        CheckstyleProjectService csService = CheckstyleProjectService.getInstance(project);
        csService.activateCheckstyleVersion(checkstyleVersion, thirdPartyClasspath);
    }

    CheckStyleConfiguration getConfiguration() {
        return CheckStyleConfiguration.getInstance(project);
    }

    private CheckerFactoryCache getCheckerFactoryCache() {
        return ServiceManager.getService(CheckerFactoryCache.class);
    }


    public void reset() {
        LOG.trace("reset() - enter");

        final PluginConfigDto pluginConfig = getConfiguration().getCurrentPluginConfig();
        configPanel.showPluginConfiguration(pluginConfig);

        activateCurrentCheckstyleVersion(pluginConfig.getCheckstyleVersion(), pluginConfig.getThirdPartyClasspath());

        LOG.trace("reset() - exit");
    }


    public void disposeUIResources() {
        // do nothing
    }
}
