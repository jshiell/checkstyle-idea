package org.infernus.idea.checkstyle;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.checker.CheckerFactoryCache;
import org.infernus.idea.checkstyle.config.PluginConfiguration;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.util.TempDirProvider;
import org.jetbrains.annotations.NotNull;

public class ConfigurationInvalidator {

    private final Project project;
    private final CheckstyleProjectService checkstyleProjectService;
    private final PluginConfigurationManager pluginConfigurationManager;
    private final CheckerFactoryCache checkerFactoryCache;

    ConfigurationInvalidator(@NotNull final Project project) {
        this.project = project;
        this.checkstyleProjectService = project.getService(CheckstyleProjectService.class);
        this.checkerFactoryCache = project.getService(CheckerFactoryCache.class);
        this.pluginConfigurationManager = project.getService(PluginConfigurationManager.class);
    }

    public void invalidateCachedResources() {
        checkerFactoryCache.invalidate();

        PluginConfiguration config = pluginConfigurationManager.getCurrent();
        config.getLocations().forEach(ConfigurationLocation::reset);

        checkstyleProjectService.activateCheckstyleVersion(config.getCheckstyleVersion(), config.getThirdPartyClasspath());
        if (!config.isCopyLibs()) {
            new TempDirProvider().deleteCopiedLibrariesDir(project);
        }
    }

}
