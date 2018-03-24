package org.infernus.idea.checkstyle;

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
public class CheckstylePluginApi {
    private final CheckStylePlugin checkstylePlugin;
    private final CheckstyleProjectService checkstyleProjectService;

    public CheckstylePluginApi(final CheckStylePlugin checkstylePlugin,
                               final CheckstyleProjectService checkstyleProjectService) {
        this.checkstylePlugin = checkstylePlugin;
        this.checkstyleProjectService = checkstyleProjectService;
    }

    @Nullable
    public ClassLoader currentCheckstyleClassLoader() {
        return checkstyleProjectService.underlyingClassLoader();
    }

    public void visitCurrentConfiguration(@NotNull final ConfigurationVisitor visitor) {
        ConfigurationLocation activeLocation = checkstylePlugin.configurationManager().getCurrent().getActiveLocation();

        if (activeLocation != null) {
            CheckstyleActions checkstyleInstance = checkstyleProjectService.getCheckstyleInstance();
            checkstyleInstance.peruseConfiguration(checkstyleInstance.loadConfiguration(activeLocation, true, new HashMap<>()),
                    module -> visitor.accept(activeLocation.getDescription(), module));
        }
    }

    @SuppressWarnings("WeakerAccess")
    public interface ConfigurationVisitor extends BiConsumer<String, ConfigurationModule> {

    }
}
