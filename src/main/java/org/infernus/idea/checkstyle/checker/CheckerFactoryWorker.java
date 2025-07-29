package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.module.Module;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;


class CheckerFactoryWorker extends Thread {
    private final ConfigurationLocation location;
    private final Map<String, String> properties;
    private final Module module;
    private final CheckstyleProjectService checkstyleProjectService;

    private Object threadReturn = null;

    CheckerFactoryWorker(@NotNull final ConfigurationLocation location,
                         @Nullable final Map<String, String> properties,
                         @Nullable final Module module,
                         @NotNull final CheckstyleProjectService checkstyleProjectService) {
        this.location = location;
        this.properties = properties;
        this.module = module;
        this.checkstyleProjectService = checkstyleProjectService;
    }

    @Override
    public void run() {
        super.run();

        try {
            final CheckStyleChecker checker = checkstyleProjectService
                    .getCheckstyleInstance()
                    .createChecker(module, location, properties);
            threadReturn = new CachedChecker(checker);
        } catch (RuntimeException e) {
            threadReturn = e;
        }
    }

    public Object getResult() {
        return threadReturn;
    }
}
