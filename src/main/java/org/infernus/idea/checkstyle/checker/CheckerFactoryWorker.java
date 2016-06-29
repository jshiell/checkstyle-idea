package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.module.Module;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertyResolver;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.InputSource;

class CheckerFactoryWorker extends Thread {
    private final Object[] threadReturn = new Object[1];

    private final ConfigurationLocation location;
    private final PropertyResolver resolver;
    private final Configurations configurations;

    CheckerFactoryWorker(@NotNull final ConfigurationLocation location,
                         @NotNull final PropertyResolver resolver,
                         @Nullable final Module module,
                         @Nullable final ClassLoader contextClassLoader) {
        this.location = location;
        this.resolver = resolver;
        this.configurations = new Configurations(location, module);

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
        try {
            Configuration config = ConfigurationLoader.loadConfiguration(new InputSource(location.resolve()), resolver, true);
            if (config == null) {
                // from the CS code this state appears to occur when there's no <module> element found
                // in the input stream
                throw new CheckstyleException("Couldn't find root module in " + location.getLocation());
            }

            config = configurations.resolveFilePaths(config);

            threadReturn[0] = new CachedChecker(createCreater(config));

        } catch (Exception e) {
            threadReturn[0] = e;
        }
    }

    @NotNull
    private CheckStyleChecker createCreater(final Configuration config)
            throws CheckstyleException {
        final com.puppycrawl.tools.checkstyle.Checker checker = new com.puppycrawl.tools.checkstyle.Checker();
        checker.setModuleClassLoader(Thread.currentThread().getContextClassLoader());
        checker.configure(config);

        return new CheckStyleChecker(checker, config, configurations.tabWidth(config), configurations.baseDir(config));
    }

}
