package org.infernus.idea.checkstyle;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleComponent;
import org.jetbrains.annotations.NotNull;

/**
 * The module level plugin.
 * <p/>
 * This is used to allow modules to override the chosen checkstyle configuration.
 */
public class CheckStyleModulePlugin implements ModuleComponent {

    private static final Logger LOG = Logger.getInstance(CheckStyleModulePlugin.class);

    public CheckStyleModulePlugin(@NotNull final Module module) {
        LOG.info("CheckStyle Module Plugin loaded for module: \"" + module.getName() + "\"");
    }

    @NotNull
    public String getComponentName() {
        return CheckStylePlugin.ID_MODULE_PLUGIN;
    }

    public void initComponent() {
    }

    public void disposeComponent() {
    }

}
