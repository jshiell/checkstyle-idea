package org.infernus.idea.checkstyle;

import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A manager for CheckStyle module configuration.
 */
@State(
        name = CheckStyleModuleConfiguration.ID_MODULE_PLUGIN,
        storages = {@Storage(StoragePathMacros.MODULE_FILE)}
)
public final class CheckStyleModuleConfiguration extends Properties
        implements PersistentStateComponent<CheckStyleModuleConfiguration.ModuleSettings> {

    private static final Logger LOG = Logger.getInstance(CheckStyleModuleConfiguration.class);

    private static final long serialVersionUID = 2804470793153632480L;

    public static final String ID_MODULE_PLUGIN = "CheckStyle-IDEA-Module";

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
            throw new IllegalArgumentException("Module is required");
        }

        this.module = module;
    }

    public void setActiveConfiguration(final ConfigurationLocation configurationLocation) {
        if (configurationLocation != null && !configurationLocations().contains(configurationLocation)) {
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
                && "true".equalsIgnoreCase(getProperty(EXCLUDE_FROM_SCAN, "false"));
    }

    public boolean isUsingModuleConfiguration() {
        return containsKey(ACTIVE_CONFIG);
    }

    public ConfigurationLocation getActiveConfiguration() {
        if (!containsKey(ACTIVE_CONFIG)) {
            return getProjectConfiguration();
        }

        ConfigurationLocation activeLocation = null;
        try {
            activeLocation = configurationLocationFactory(module.getProject()).create(module.getProject(), getProperty(ACTIVE_CONFIG));
        } catch (IllegalArgumentException e) {
            LOG.warn("Could not load active configuration", e);
        }

        if (activeLocation == null || !configurationLocations().contains(activeLocation)) {
            LOG.info("Active module configuration is invalid, returning project configuration");
            return getProjectConfiguration();
        }

        return activeLocation;
    }

    private ConfigurationLocation getProjectConfiguration() {
        return configurationManager().getCurrent().getActiveLocation();
    }

    private PluginConfigurationManager configurationManager() {
        return ServiceManager.getService(module.getProject(), PluginConfigurationManager.class);
    }

    public List<ConfigurationLocation> configurationLocations() {
        return new ArrayList<>(configurationManager().getCurrent().getLocations());
    }

    public List<ConfigurationLocation> getAndResolveConfigurationLocations() {
        return new ArrayList<>(configurationManager().getCurrent().getLocations());
    }

    private ConfigurationLocationFactory configurationLocationFactory(final Project project) {
        return ServiceManager.getService(project, ConfigurationLocationFactory.class);
    }

    public ModuleSettings getState() {
        final Map<String, String> moduleConfiguration = new HashMap<>();
        for (String configurationKey : stringPropertyNames()) {
            moduleConfiguration.put(configurationKey, getProperty(configurationKey));
        }
        return ModuleSettings.create(moduleConfiguration);
    }

    public void loadState(@NotNull final ModuleSettings sourceModuleSettings) {
        clear();

        for (final String key : sourceModuleSettings.configuration().keySet()) {
            setProperty(key, sourceModuleSettings.configuration().get(key));
        }
    }

    @Override
    public synchronized boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        CheckStyleModuleConfiguration that = (CheckStyleModuleConfiguration) o;

        return module.equals(that.module);
    }

    @Override
    public synchronized int hashCode() {
        return 31 * super.hashCode() + module.hashCode();
    }

    static class ModuleSettings {
        @MapAnnotation
        private Map<String, String> configuration;

        static ModuleSettings create(final Map<String, String> configuration) {
            final ModuleSettings moduleSettings = new ModuleSettings();
            moduleSettings.configuration = configuration;
            return moduleSettings;
        }

        @NotNull
        public Map<String, String> configuration() {
            if (configuration == null) {
                return Collections.emptyMap();
            }
            return configuration;
        }
    }
}
