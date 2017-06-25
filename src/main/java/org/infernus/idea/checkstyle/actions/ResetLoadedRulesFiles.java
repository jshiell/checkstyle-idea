package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.infernus.idea.checkstyle.CheckStyleConfiguration;
import org.infernus.idea.checkstyle.checker.CheckerFactoryCache;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.components.ServiceManager.getService;

/**
 * Clear the Checker cache and blacklists, forcing a reload of rules files.
 */
public class ResetLoadedRulesFiles extends BaseAction {

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        project(event).ifPresent(project -> getService(project, CheckStyleConfiguration.class)
                .getCurrentPluginConfig().getLocations()
                .forEach(ConfigurationLocation::removeFromBlacklist));

        getService(CheckerFactoryCache.class).invalidate();
    }
}
