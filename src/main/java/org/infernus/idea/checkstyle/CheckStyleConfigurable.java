package org.infernus.idea.checkstyle;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.checker.CheckerFactoryCache;
import org.infernus.idea.checkstyle.config.PluginConfiguration;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.ui.CheckStyleConfigPanel;
import org.infernus.idea.checkstyle.util.TempDirProvider;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;


/**
 * The "configurable component" required by IntelliJ IDEA to provide a Swing form for inclusion into the 'Settings'
 * dialog. Registered in {@code plugin.xml} as a {@code projectConfigurable} extension.
 */
public class CheckStyleConfigurable
        implements Configurable {
    private static final Logger LOG = Logger.getInstance(CheckStyleConfigurable.class);

    private final Project project;

    private final CheckStyleConfigPanel configPanel;
    private final CheckstyleProjectService checkstyleProjectService;
    private final PluginConfigurationManager pluginConfigurationManager;
    private final CheckerFactoryCache checkerFactoryCache;

    CheckStyleConfigurable(@NotNull final Project project) {
        this.project = project;

        this.checkstyleProjectService = project.getService(CheckstyleProjectService.class);
        this.checkerFactoryCache = project.getService(CheckerFactoryCache.class);
        this.pluginConfigurationManager = project.getService(PluginConfigurationManager.class);

        this.configPanel = new CheckStyleConfigPanel(project, checkstyleProjectService, checkerFactoryCache);
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
        final PluginConfiguration newConfig = PluginConfigurationBuilder.from(configPanel.getPluginConfiguration())
                .withScanBeforeCheckin(pluginConfigurationManager.getCurrent().isScanBeforeCheckin())
                .build();
        pluginConfigurationManager.setCurrent(newConfig, true);

        activateCurrentCheckstyleVersion(newConfig.getCheckstyleVersion(), newConfig.getThirdPartyClasspath());
        if (!newConfig.isCopyLibs()) {
            new TempDirProvider().deleteCopiedLibrariesDir(project);
        }
    }

    private void activateCurrentCheckstyleVersion(final String checkstyleVersion,
                                                  final List<String> thirdPartyClasspath) {
        invalidateCachedResources();
        checkstyleProjectService.activateCheckstyleVersion(checkstyleVersion, thirdPartyClasspath);
    }

    private void invalidateCachedResources() {
        checkerFactoryCache.invalidate();
        pluginConfigurationManager
                .getCurrent()
                .getLocations()
                .forEach(ConfigurationLocation::reset);
    }

    public void reset() {
        final PluginConfiguration pluginConfig = pluginConfigurationManager.getCurrent();
        configPanel.showPluginConfiguration(pluginConfig);

        activateCurrentCheckstyleVersion(pluginConfig.getCheckstyleVersion(), pluginConfig.getThirdPartyClasspath());
    }

    public void disposeUIResources() {
        // do nothing
    }
}
