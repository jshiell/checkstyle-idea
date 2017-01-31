package org.infernus.idea.checkstyle;

import java.io.IOException;
import java.util.List;
import javax.swing.JComponent;

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
        try {
            boolean result = haveLocationsChanged(configuration) || hasActiveLocationChanged(configuration) ||
                    !configuration.getThirdPartyClassPath().equals(configPanel.getThirdPartyClasspath()) ||
                    configuration.getScanScope() != configPanel.getScanScope() || configuration.isSuppressingErrors()
                    != configPanel.isSuppressingErrors() || !configuration.getCheckstyleVersion(null).equals(configPanel
                    .getCheckstyleVersion());
            if (LOG.isTraceEnabled()) {
                LOG.trace("isModified() - exit - result=" + result);
            }
            return result;
        } catch (IOException e) {
            LOG.error("Failed to read properties from one of " + configPanel.getConfigurationLocations(), e);
            Notifications.showError(project, CheckStyleBundle.message("checkstyle.file-not-found"));
            LOG.trace("isModified() - exit - result=true");
            return true;
        }
    }

    private boolean hasActiveLocationChanged(final CheckStyleConfiguration pConfiguration) throws IOException {
        final ConfigurationLocation configActiveLocation = pConfiguration.getActiveConfiguration();
        final ConfigurationLocation panelActiveLocation = configPanel.getActiveLocation();
        boolean result = false;
        if (configActiveLocation == null) {
            result = panelActiveLocation != null;
        } else {
            result = configActiveLocation.hasChangedFrom(panelActiveLocation);
        }
        return result;
    }

    private boolean haveLocationsChanged(final CheckStyleConfiguration pCurrentConfiguration) {
        LOG.trace("haveLocationsChanged() - enter");
        final List<ConfigurationLocation> configLocations = pCurrentConfiguration.configurationLocations();
        final List<ConfigurationLocation> panelLocations = configPanel.getConfigurationLocations();
        boolean result = !Objects.equals(configLocations, panelLocations);
        if (LOG.isTraceEnabled()) {
            LOG.trace("haveLocationsChanged() - exit - result=" + result);
        }
        return result;
    }

    public void apply() throws ConfigurationException {
        LOG.trace("apply() - enter");
        final CheckStyleConfiguration configuration = getConfiguration();
        configuration.setCheckstyleVersion(configPanel.getCheckstyleVersion());
        configuration.setConfigurationLocations(configPanel.getConfigurationLocations());
        configuration.setActiveConfiguration(configPanel.getActiveLocation());
        configuration.setScanScope(configPanel.getScanScope());
        configuration.setSuppressingErrors(configPanel.isSuppressingErrors());

        final List<String> thirdPartyClasspath = configPanel.getThirdPartyClasspath();
        configuration.setThirdPartyClassPath(thirdPartyClasspath);

        // Invalidate cache *before* activating the new Checkstyle version
        getCheckerFactoryCache().invalidate();

        CheckstyleProjectService csService = CheckstyleProjectService.getInstance(project);
        csService.activateCheckstyleVersion(configPanel.getCheckstyleVersion(), thirdPartyClasspath);

        LOG.trace("apply() - exit");
    }

    CheckStyleConfiguration getConfiguration() {
        return CheckStyleConfiguration.getInstance(project);
    }

    private CheckerFactoryCache getCheckerFactoryCache() {
        return ServiceManager.getService(CheckerFactoryCache.class);
    }

    public void reset() {
        LOG.trace("reset() - enter");
        final CheckStyleConfiguration configuration = getConfiguration();
        configPanel.setCheckstyleVersion(configuration.getCheckstyleVersion(null));
        configPanel.setConfigurationLocations(configuration.getAndResolveConfigurationLocations());
        configPanel.setActiveLocation(configuration.getActiveConfiguration());
        configPanel.setScanScope(configuration.getScanScope());
        configPanel.setSuppressingErrors(configuration.isSuppressingErrors());
        configPanel.setThirdPartyClasspath(configuration.getThirdPartyClassPath());
        LOG.trace("reset() - exit");
    }

    public void disposeUIResources() {
        // do nothing
    }
}
