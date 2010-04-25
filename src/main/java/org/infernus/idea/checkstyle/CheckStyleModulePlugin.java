package org.infernus.idea.checkstyle;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.util.IDEAUtilities;
import org.infernus.idea.checkstyle.ui.CheckStyleModuleConfigPanel;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * The module level plugin.
 * <p/>
 * This is used to allow modules to override the chosen checkstyle configuration.
 */
@State(
        name = CheckStyleConstants.ID_MODULE_PLUGIN,
        storages = {
                @Storage(
                        id = "other",
                        file = "$MODULE_FILE$"
                )}
)
public class CheckStyleModulePlugin implements ModuleComponent, Configurable,
        PersistentStateComponent<CheckStyleModulePlugin.ConfigurationBean> {

    @NonNls
    private static final Log LOG = LogFactory.getLog(CheckStyleModulePlugin.class);

    private CheckStyleModuleConfiguration configuration;
    private Module module;
    private CheckStyleModuleConfigPanel configPanel;

    /**
     * Construct a plug-in instance for the given module.
     *
     * @param module the current module.
     */
    public CheckStyleModulePlugin(final Module module) {
        if (module == null) {
            throw new IllegalStateException("Module may not be null");
        }

        this.module = module;

        try {
            LOG.info("CheckStyle Module Plugin loaded for module: \""
                    + module.getName() + "\"");

            this.configuration = new CheckStyleModuleConfiguration(module);

        } catch (Throwable t) {
            LOG.error("Project initialisation failed.", t);
        }
    }

    public CheckStyleModuleConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * {@inheritDoc}
     */
    public CheckStyleModulePlugin.ConfigurationBean getState() {
        final ConfigurationBean configBean = new ConfigurationBean();
        for (final Enumeration confNames = configuration.propertyNames(); confNames.hasMoreElements();) {
            final String elementName = (String) confNames.nextElement();
            configBean.configuration.put(elementName, configuration.getProperty(elementName));
        }
        return configBean;
    }

    /**
     * {@inheritDoc}
     */
    public void loadState(final CheckStyleModulePlugin.ConfigurationBean newConfiguration) {
        configuration.clear();

        if (newConfiguration != null && newConfiguration.configuration != null) {
            for (final String key : newConfiguration.configuration.keySet()) {
                configuration.setProperty(key, newConfiguration.configuration.get(key));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void projectOpened() {
        LOG.debug("Project opened.");
    }

    /**
     * {@inheritDoc}
     */
    public void projectClosed() {
        LOG.debug("Project closed.");
    }

    /**
     * {@inheritDoc}
     */
    public void moduleAdded() {
        LOG.debug("Moduled added: " + module.getName());
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    public String getComponentName() {
        return CheckStyleConstants.ID_MODULE_PLUGIN;
    }

    /**
     * {@inheritDoc}
     */
    public void initComponent() {
    }

    /**
     * {@inheritDoc}
     */
    public void disposeComponent() {
    }

    /**
     * {@inheritDoc}
     */
    public String getDisplayName() {
        return IDEAUtilities.getResource("plugin.configuration-name",
                "CheckStyle Plugin");
    }

    /**
     * {@inheritDoc}
     */
    public Icon getIcon() {
        return IDEAUtilities.getIcon(
                "/org/infernus/idea/checkstyle/images/checkstyle16.png");
    }

    /**
     * {@inheritDoc}
     */
    public String getHelpTopic() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public JComponent createComponent() {
        if (configPanel == null) {
            configPanel = new CheckStyleModuleConfigPanel();
        }

        reset();

        return configPanel;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isModified() {
        if (configPanel == null) {
            return false;
        }

        return configPanel.isModified();
    }

    /**
     * {@inheritDoc}
     */
    public void apply() throws ConfigurationException {
        if (configPanel == null) {
            return;
        }

        configuration.setActiveConfiguration(configPanel.getActiveLocation());

        reset(); // reset modification state
    }

    /**
     * {@inheritDoc}
     */
    public void reset() {
        if (configPanel == null) {
            return;
        }

        configPanel.setConfigurationLocations(configuration.getConfigurationLocations());

        if (configuration.isUsingModuleConfiguration()) {
            configPanel.setActiveLocation(configuration.getActiveConfiguration());
        } else {
            configPanel.setActiveLocation(null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void disposeUIResources() {
        configPanel = null;
    }

    /**
     * Wrapper class for IDEA state serialisation.
     */
    public static class ConfigurationBean {
        public Map<String, String> configuration = new HashMap<String, String>();
    }

}
