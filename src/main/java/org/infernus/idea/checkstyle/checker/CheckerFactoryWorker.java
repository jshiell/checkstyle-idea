package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.module.Module;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;


class CheckerFactoryWorker
        extends Thread
{
    private static final Log LOG = LogFactory.getLog(CheckerFactory.class);

    private final ConfigurationLocation location;
    private final Map<String, String> properties;
    private final Module module;
    private ClassLoader loaderOfCheckedCode;

    private Object threadReturn = null;


    CheckerFactoryWorker(@NotNull final ConfigurationLocation location, @Nullable Map<String, String> pProperties,
                         @Nullable final Module pModule, @NotNull final ClassLoader pLoaderOfCheckedCode) {
        this.location = location;
        this.properties = pProperties;
        this.module = pModule;
        if (pLoaderOfCheckedCode == null) {
            throw new IllegalArgumentException("internal error - class loader for loading checked code is unavailable");
        }
        this.loaderOfCheckedCode = pLoaderOfCheckedCode;
    }


    @Override
    public void run() {
        super.run();

        setContextClassLoader(loaderOfCheckedCode);

        final CheckstyleProjectService csService = CheckstyleProjectService.getInstance(module.getProject());
        try {
            final CheckStyleChecker checker = csService.getCheckstyleInstance().createChecker(module, location,
                    properties, loaderOfCheckedCode);
            threadReturn = new CachedChecker(module.getProject(), checker);
        } catch (RuntimeException e) {
            threadReturn = e;
        }
    }


    public Object getResult() {
        return threadReturn;
    }
}
