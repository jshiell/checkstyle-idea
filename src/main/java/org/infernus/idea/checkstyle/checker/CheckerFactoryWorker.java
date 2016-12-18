package org.infernus.idea.checkstyle.checker;

import java.util.Map;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


class CheckerFactoryWorker
        extends Thread
{
    // TODO Why use an array here?
    private final Object[] threadReturn = new Object[1];

    private final ConfigurationLocation location;
    private final Map<String, String> properties;
    private final Module module;


    // TODO The ClassLoader argument should be superfluous, as we use a custom classloader from here on, anyway.
    CheckerFactoryWorker(@NotNull final ConfigurationLocation location, @NotNull Map<String, String> pProperties,
                         @Nullable final Module pModule, @Nullable final ClassLoader contextClassLoader) {
        this.location = location;
        properties = pProperties;
        this.module = pModule;

        if (contextClassLoader != null) {
            setContextClassLoader(contextClassLoader);
        } else {
            setContextClassLoader(getClass().getClassLoader());
        }
    }


    public Object getResult() {
        return threadReturn[0];
    }


    @Override
    public void run() {

        super.run();
        final CheckstyleProjectService csService = ServiceManager.getService(module.getProject(),
                CheckstyleProjectService.class);
        try {
            threadReturn[0] = csService.getCheckstyleInstance().createChecker(module, location, properties);
        } catch (RuntimeException e) {
            threadReturn[0] = e;
        }
    }
}
