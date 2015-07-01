package org.infernus.idea.checkstyle;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;

import java.util.*;

/**
 * A manager for CheckStyle module configuration.
 */
@State(
        name = CheckStylePlugin.ID_MODULE_PLUGIN,
        storages = {@Storage(id = "other", file = "$MODULE_FILE$")}
)
public final class CheckStyleModuleConfiguration extends Properties
        implements PersistentStateComponent<CheckStyleModuleConfiguration.ModuleSettings> {

    private static final Log LOG = LogFactory.getLog(CheckStyleModuleConfiguration.class);

    private static final long serialVersionUID = 2804470793153632480L;

    private static final String ACTIVE_CONFIG = "active-configuration";
    private static final String EXCLUDE_FROM_SCAN = "exclude-from-scan";

    private final Module module;

    /**
     * Create a new configuration bean.
     *
     * @param module the module we belong to.
     */
    public CheckStyleModuleConfiguration(final Module module) {
        if (module == null) {
            throw new IllegalArgumentException("Project is required");
        }

        this.module = module;
    }

    public void setActiveConfiguration(final ConfigurationLocation configurationLocation) {
        final List<ConfigurationLocation> configurationLocations = getConfigurationLocations();

        if (configurationLocation != null && !configurationLocations.contains(configurationLocation)) {
            throw new IllegalArgumentException("Location is not valid: " + configurationLocation);
        }

        if (configurationLocation != null) {
            setProperty(ACTIVE_CONFIG, configurationLocation.getDescriptor());
        } else {
            remove(ACTIVE_CONFIG);
        }
    }

    public void setExcluded(final boolean excluded) {
        if (excluded) {
            setProperty(EXCLUDE_FROM_SCAN, "true");
        } else {
            remove(EXCLUDE_FROM_SCAN);
        }
    }

    public boolean isExcluded() {
        return containsKey(EXCLUDE_FROM_SCAN)
                && getProperty(EXCLUDE_FROM_SCAN, "false").equalsIgnoreCase("true");
    }

    public boolean isUsingModuleConfiguration() {
        return containsKey(ACTIVE_CONFIG);
    }

    public ConfigurationLocation getActiveConfiguration() {
        final List<ConfigurationLocation> configurationLocations = getConfigurationLocations();

        if (!containsKey(ACTIVE_CONFIG)) {
            return getProjectConfiguration();
        }

        ConfigurationLocation activeLocation = null;
        try {
            activeLocation = configurationLocationFactory(module.getProject()).create(module.getProject(), getProperty(ACTIVE_CONFIG));
        } catch (IllegalArgumentException e) {
            LOG.warn("Could not load active configuration", e);
        }

        if (activeLocation == null || !configurationLocations.contains(activeLocation)) {
            LOG.info("Active module configuration is invalid, returning project configuration");
            return getProjectConfiguration();
        }

        return activeLocation;
    }

    private ConfigurationLocationFactory configurationLocationFactory(final Project project) {
        return ServiceManager.getService(project, ConfigurationLocationFactory.class);
    }

    private ConfigurationLocation getProjectConfiguration() {
        final CheckStylePlugin checkStylePlugin
                = module.getProject().getComponent(CheckStylePlugin.class);
        if (checkStylePlugin == null) {
            throw new IllegalStateException("Couldn't get checkstyle plugin");
        }

        return checkStylePlugin.getConfiguration().getActiveConfiguration();
    }

    public List<ConfigurationLocation> getConfigurationLocations() {
        final CheckStylePlugin checkStylePlugin
                = module.getProject().getComponent(CheckStylePlugin.class);
        if (checkStylePlugin == null) {
            throw new IllegalStateException("Couldn't get checkstyle plugin");
        }

        return checkStylePlugin.getConfiguration().getConfigurationLocations();
    }

    public ModuleSettings getState() {
        final ModuleSettings moduleSettings = new ModuleSettings();
        for (String configurationKey : stringPropertyNames()) {
            moduleSettings.configuration.put(configurationKey, getProperty(configurationKey));
        }
        return moduleSettings;
    }

    public void loadState(final ModuleSettings moduleSettings) {
        clear();

        if (moduleSettings != null && moduleSettings.configuration != null) {
            for (final String key : moduleSettings.configuration.keySet()) {
                setProperty(key, moduleSettings.configuration.get(key));
            }
        }
    }

    /**
     * Wrapper class for IDEA state serialisation.
     */
    public static class ModuleSettings {
        public Map<String, String> configuration = new HashMap<String, String>();

        public ModuleSettings() {
            this.configuration = new HashMap<String, String>();
        }

        public ModuleSettings(final Map<String, String> configuration) {
            this.configuration = configuration;
        }

        public Map<String, String> configurationAsMap() {
            if (configuration == null) {
                return Collections.emptyMap();
            }
            return configuration;
        }
    }
}
