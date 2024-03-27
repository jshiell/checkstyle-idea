package org.infernus.idea.checkstyle.startup;

import com.intellij.ide.ui.IdeUiService;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.util.Notifications;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import static org.infernus.idea.checkstyle.CheckStyleBundle.message;
import static org.infernus.idea.checkstyle.CheckStylePlugin.version;

public class NotifyUserIfPluginUpdated implements StartupActivity {

    private static final Logger LOG = Logger.getInstance(NotifyUserIfPluginUpdated.class);

    @Override
    public void runActivity(@NotNull final Project project) {
        final PluginConfigurationManager pluginConfigurationManager = pluginConfigurationManager(project);
        if (!Objects.equals(version(), lastActivePluginVersion(pluginConfigurationManager))) {
            Notifications.showInfo(project, message("plugin.update"), new NotificationAction(message("plugin.update.action")) {
                @SuppressWarnings("UnstableApiUsage")
                @Override
                public void actionPerformed(@NotNull final AnActionEvent event,
                                            @NotNull final Notification notification) {
                    try {
                        final URL updateUrl = new URL("https://github.com/jshiell/checkstyle-idea/releases/tag/%s".formatted(version()));
                        IdeUiService.getInstance().browse(updateUrl);
                        notification.expire();
                    } catch (MalformedURLException e) {
                        LOG.error("Failed to display update message", e);
                    }
                }
            });

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
