package org.infernus.idea.checkstyle;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.util.Notifications;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static org.infernus.idea.checkstyle.CheckStyleBundle.message;
import static org.infernus.idea.checkstyle.CheckStylePlugin.currentPluginVersion;

public class NotifyUserIfPluginUpdated implements StartupActivity {

    @Override
    public void runActivity(@NotNull final Project project) {
        if (!Objects.equals(currentPluginVersion(), lastActivePluginVersion(project))) {
            Notifications.showInfo(project, message("plugin.update", currentPluginVersion()));

            PluginConfigurationManager configurationManager = pluginConfigurationManager(project);
            configurationManager.setCurrent(PluginConfigurationBuilder.from(configurationManager.getCurrent())
                    .withLastActivePluginVersion(currentPluginVersion())
                    .build(), false);
        }
    }

    private PluginConfigurationManager pluginConfigurationManager(@NotNull final Project project) {
        return project.getComponent(PluginConfigurationManager.class);
    }

    private String lastActivePluginVersion(final Project project) {
        return pluginConfigurationManager(project).getCurrent().getLastActivePluginVersion();
    }

}
