package org.infernus.idea.checkstyle;

import com.intellij.openapi.module.Module;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.jetbrains.annotations.NonNls;

import java.util.List;
import java.util.Properties;

/**
 * A manager for CheckStyle module configuration.
 *
 * @author James Shiell
 * @version 1.0
 */
public final class CheckStyleModuleConfiguration extends Properties {

    @NonNls
    private static final Log LOG = LogFactory.getLog(CheckStyleModuleConfiguration.class);

    private static final long serialVersionUID = 2804470793153632480L;

    private static final String ACTIVE_CONFIG = "active-configuration";

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
            activeLocation = ConfigurationLocationFactory.create(module.getProject(), getProperty(ACTIVE_CONFIG));
        } catch (IllegalArgumentException e) {
            LOG.warn("Could not load active configuration", e);
        }

        if (activeLocation == null || !configurationLocations.contains(activeLocation)) {
            LOG.info("Active module configuration is invalid, returning project configuration");
            return getProjectConfiguration();
        }

        return activeLocation;
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
}