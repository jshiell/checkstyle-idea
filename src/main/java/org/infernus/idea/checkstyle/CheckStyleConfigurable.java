package org.infernus.idea.checkstyle;

import java.util.List;
import javax.swing.JComponent;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.checker.CheckerFactoryCache;
import org.infernus.idea.checkstyle.config.PluginConfigDto;
import org.infernus.idea.checkstyle.config.PluginConfigDtoBuilder;
import org.infernus.idea.checkstyle.ui.CheckStyleConfigPanel;
import org.infernus.idea.checkstyle.util.TempDirProvider;
import org.jetbrains.annotations.NotNull;


/**
 * The "configurable component" required by IntelliJ IDEA to provide a Swing form for inclusion into the 'Settings'
 * dialog. Registered in {@code plugin.xml} as a {@code projectConfigurable} extension.
 */
public class CheckStyleConfigurable
        implements Configurable {
    private static final Logger LOG = Logger.getInstance(CheckStyleConfigurable.class);

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
        LOG.debug("isModified() - enter");
        final CheckStyleConfiguration configuration = getConfiguration();
        final PluginConfigDto oldConfig = configuration.getCurrent();
        final PluginConfigDto newConfig = PluginConfigDtoBuilder
                .from(configPanel.getPluginConfiguration())
                .withScanBeforeCheckin(oldConfig.isScanBeforeCheckin())
                .build();

        boolean result = !oldConfig.hasChangedFrom(newConfig);
        if (LOG.isDebugEnabled()) {
            LOG.debug("isModified() - exit - result=" + result);
        }
        return result;
    }


    public void apply() throws ConfigurationException {
        LOG.debug("apply() - enter");

        final CheckStyleConfiguration configuration = getConfiguration();
        final PluginConfigDto newConfig = PluginConfigDtoBuilder.from(configPanel.getPluginConfiguration())
                .withScanBeforeCheckin(configuration.getCurrent().isScanBeforeCheckin())
                .build();
        configuration.setCurrent(newConfig, true);

        activateCurrentCheckstyleVersion(newConfig.getCheckstyleVersion(), newConfig.getThirdPartyClasspath());
        if (!newConfig.isCopyLibs()) {
            new TempDirProvider().deleteCopiedLibrariesDir(project);
        }

        LOG.debug("apply() - exit");
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
        return ServiceManager.getService(project, CheckerFactoryCache.class);
    }


    public void reset() {
        LOG.debug("reset() - enter");

        final PluginConfigDto pluginConfig = getConfiguration().getCurrent();
        configPanel.showPluginConfiguration(pluginConfig);

        activateCurrentCheckstyleVersion(pluginConfig.getCheckstyleVersion(), pluginConfig.getThirdPartyClasspath());

        LOG.debug("reset() - exit");
    }


    public void disposeUIResources() {
        // do nothing
    }
}
