package org.infernus.idea.checkstyle;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Main class for the CheckStyle scanning plug-in.
 * <p>
 * TODO split out config location source
 */
public final class CheckStylePlugin {

    /**
     * The plugin ID. Caution: It must be identical to the String set in build.gradle at intellij.pluginName
     */
    public static final String ID_PLUGIN = "CheckStyle-IDEA";

    private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(CheckStylePlugin.class);

    private final Project project;

    /**
     * Construct a plug-in instance for the given project.
     *
     * @param project the current project.
     */
    public CheckStylePlugin(@NotNull final Project project) {
        this.project = project;
    }


    public static String currentPluginVersion() {
        final IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId(ID_PLUGIN));
        if (plugin != null) {
            return plugin.getVersion();
        }
        return "unknown";
    }

    public static void processErrorAndLog(@NotNull final String action, @NotNull final Throwable e) {
        LOG.warn(action + " failed", e);
    }

    public ConfigurationLocation getConfigurationLocation(@Nullable final Module module,
                                                          @Nullable final ConfigurationLocation override) {
        if (override != null) {
            return override;
        }

        if (module != null) {
            final CheckStyleModuleConfiguration moduleConfiguration = ModuleServiceManager
                    .getService(module, CheckStyleModuleConfiguration.class);
            if (moduleConfiguration == null) {
                throw new IllegalStateException("Couldn't get checkstyle module configuration");
            }

            if (moduleConfiguration.isExcluded()) {
                return null;
            }
            return moduleConfiguration.getActiveConfiguration();
        }
        return configurationManager().getCurrent().getActiveLocation();
    }

    private PluginConfigurationManager configurationManager() {
        return ServiceManager.getService(project, PluginConfigurationManager.class);
    }
}
