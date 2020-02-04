package org.infernus.idea.checkstyle;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.csapi.CheckstyleActions;
import org.infernus.idea.checkstyle.csapi.ConfigurationModule;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.function.BiConsumer;

/**
 * This class contains methods known to be used by external dependencies.
 * <p/>
 * We guarantee nothing, but try to keep compatibility :-)
 */
@SuppressWarnings("unused")
public class CheckstylePluginApi {
    private final Project project;

    public CheckstylePluginApi(final Project project) {
        this.project = project;
    }

    @Nullable
    public ClassLoader currentCheckstyleClassLoader() {
        return checkstyleProjectService().underlyingClassLoader();
    }

    public void visitCurrentConfiguration(@NotNull final ConfigurationVisitor visitor) {
        ConfigurationLocation activeLocation = pluginConfigurationManager().getCurrent().getActiveLocation();

        if (activeLocation != null) {
            CheckstyleActions checkstyleInstance = checkstyleProjectService().getCheckstyleInstance();
            checkstyleInstance.peruseConfiguration(checkstyleInstance.loadConfiguration(activeLocation, true, new HashMap<>()),
                    module -> visitor.accept(activeLocation.getDescription(), module));
        }
    }

    @SuppressWarnings("WeakerAccess")
    public interface ConfigurationVisitor extends BiConsumer<String, ConfigurationModule> {

    }

    private PluginConfigurationManager pluginConfigurationManager() {
        return ServiceManager.getService(project, PluginConfigurationManager.class);
    }

    private CheckstyleProjectService checkstyleProjectService() {
        return ServiceManager.getService(project, CheckstyleProjectService.class);
    }
}
