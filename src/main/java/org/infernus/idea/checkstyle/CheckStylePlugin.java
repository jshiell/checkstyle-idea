package org.infernus.idea.checkstyle;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;

public final class CheckStylePlugin {

    private CheckStylePlugin() {
    }

    /**
     * The plugin ID. Caution: It must be identical to the String set in build.gradle at intellij.pluginName
     */
    public static final String ID_PLUGIN = "CheckStyle-IDEA";

    public static String version() {
        try {
            final IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId(ID_PLUGIN));
            if (plugin != null) {
                return plugin.getVersion();
            }
        } catch (Throwable ignored) {
            // thrown if plugin isn't initialised, e.g. during tests
        }
        return "unknown";
    }

}
