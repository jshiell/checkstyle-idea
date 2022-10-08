package org.infernus.idea.checkstyle.startup;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.util.Notifications;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static org.infernus.idea.checkstyle.CheckStyleBundle.message;
import static org.infernus.idea.checkstyle.CheckStylePlugin.version;

public class NotifyUserIfPluginUpdated implements StartupActivity {

    @Override
    public void runActivity(@NotNull final Project project) {
        final PluginConfigurationManager pluginConfigurationManager = pluginConfigurationManager(project);
        if (!Objects.equals(version(), lastActivePluginVersion(pluginConfigurationManager))) {
            Notifications.showInfo(project, message("plugin.update", version()));

            pluginConfigurationManager.setCurrent(PluginConfigurationBuilder.from(pluginConfigurationManager.getCurrent())
                    .withLastActivePluginVersion(version())
                    .build(), false);
        }
    }

    private String lastActivePluginVersion(final PluginConfigurationManager pluginConfigurationManager) {
        return pluginConfigurationManager.getCurrent().getLastActivePluginVersion();
    }

    private PluginConfigurationManager pluginConfigurationManager(final Project project) {
        return project.getService(PluginConfigurationManager.class);
    }

}
