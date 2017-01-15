package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.exception.CheckstyleToolException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.util.Notifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.infernus.idea.checkstyle.util.Strings.isBlank;


/**
 * Creates Checkers. Registered as projectService in {@code plugin.xml}.
 */
public class CheckerFactory
{
    private static final Log LOG = LogFactory.getLog(CheckerFactory.class);

    private final Project project;
    private final CheckerFactoryCache cache;


    public CheckerFactory(@NotNull final Project project, @NotNull final CheckerFactoryCache cache) {
        this.project = project;
        this.cache = cache;
    }


    public void verify(final ConfigurationLocation location) throws IOException {
        checker(null, location);
    }


    public Optional<CheckStyleChecker> checker(@Nullable final Module module, @NotNull final ConfigurationLocation
            location) {
        LOG.debug("Getting CheckStyle checker with location " + location);

        if (location == null) {
            return Optional.empty();
        }

        try {
            final CachedChecker cachedChecker = getOrCreateCachedChecker(location, module);
            if (cachedChecker != null) {
                return Optional.of(cachedChecker.getCheckStyleChecker());
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new CheckStylePluginException("Couldn't create Checker from " + location, e);
        }
    }


    private CachedChecker getOrCreateCachedChecker(@NotNull final ConfigurationLocation location, @Nullable final
    Module module) throws IOException {
        final Optional<CachedChecker> cachedChecker = cache.get(location, module);
        if (cachedChecker.isPresent()) {
            return cachedChecker.get();
        }

        final ListPropertyResolver propertyResolver = new ListPropertyResolver(addEclipseCsProperties(location,
                module));
        final CachedChecker checker = createChecker(location, module, propertyResolver);
        if (checker != null) {
            cache.put(location, module, checker);
            return checker;
        }

        return null;
    }


    private Map<String, String> addEclipseCsProperties(final ConfigurationLocation location, final Module module)
            throws IOException {
        final Map<String, String> properties = new HashMap<>(location.getProperties());

        addIfAbsent("basedir", basePathFor(module), properties);

        addIfAbsent("project_loc", project.getBasePath(), properties);
        addIfAbsent("workspace_loc", project.getBasePath(), properties);

        final String locationBaseDir = Optional.ofNullable(location.getBaseDir()).map(File::toString).orElseGet
                (project::getBasePath);
        addIfAbsent("config_loc", locationBaseDir, properties);
        addIfAbsent("samedir", locationBaseDir, properties);

        return properties;
    }

    private String basePathFor(final Module module) {
        if (module != null) {
            final File moduleFile = new File(module.getModuleFilePath());
            if (moduleFile.getParent() != null && moduleFile.getParentFile().exists()) {
                return moduleFile.getParentFile().getAbsolutePath();
            }
        }
        return project.getBasePath();
    }

    private void addIfAbsent(final String key, final String value, final Map<String, String> properties) {
        if (isBlank(properties.get(key))) {
            properties.put(key, value);
        }
    }


    private ModuleClassPathBuilder moduleClassPathBuilder() {
        return ServiceManager.getService(project, ModuleClassPathBuilder.class);
    }

    private CachedChecker createChecker(@NotNull final ConfigurationLocation location, @Nullable final Module module,
                                        final ListPropertyResolver resolver) {

        final ClassLoader loaderOfCheckedCode = moduleClassPathBuilder().build(module);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Call to create new checker.");
            logProperties(resolver);
            logClassLoaders(loaderOfCheckedCode);
        }

        final Object workerResult = executeWorker(location, module, resolver, loaderOfCheckedCode);

        if (workerResult instanceof CheckstyleToolException) {
            return blacklistAndShowMessage(location, module, (CheckstyleToolException) workerResult);
        } else if (workerResult instanceof IOException) {
            LOG.info("CheckStyle configuration could not be loaded: " + location.getLocation(), (IOException)
                    workerResult);
            return blacklistAndShowMessage(location, module, "checkstyle.file-not-found", location.getLocation());
        } else if (workerResult instanceof Throwable) {
            location.blacklist();
            throw new CheckStylePluginException("Could not load configuration", (Throwable) workerResult);
        }

        return (CachedChecker) workerResult;
    }


    private Object executeWorker(@NotNull final ConfigurationLocation location, @Nullable final Module module, final
    ListPropertyResolver resolver, @NotNull final ClassLoader pLoaderOfCheckedCode) {
        final CheckerFactoryWorker worker = new CheckerFactoryWorker(location, resolver.getPropertyNamesToValues(),
                module, pLoaderOfCheckedCode);
        worker.start();

        while (worker.isAlive()) {
            try {
                worker.join();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        return worker.getResult();
    }


    private CachedChecker blacklistAndShowMessage(final ConfigurationLocation location, final Module module, final
    String messageKey, final Object... messageArgs) {
        if (!location.isBlacklisted()) {
            location.blacklist();

            if (module != null) {
                Notifications.showError(module.getProject(), CheckStyleBundle.message(messageKey, messageArgs));
            } else {
                throw new CheckStylePluginException(CheckStyleBundle.message(messageKey, messageArgs));
            }
        }
        return null;
    }

    private CachedChecker blacklistAndShowMessage(final ConfigurationLocation location, final Module module, final
    CheckstyleToolException checkstyleException) {
        if (checkstyleException.getMessage().contains("Unable to instantiate DoubleCheckedLocking")) {
            return blacklistAndShowMessage(location, module, "checkstyle.double-checked-locking");
        } else if (checkstyleException.getMessage().contains("unable to parse configuration stream") &&
                checkstyleException.getCause() != null) {
            return blacklistAndShowMessage(location, module, checkstyleException.getCause().getMessage());
        }

        return blacklistAndShowMessage(location, module, "checkstyle.parse-failed", checkstyleException.getMessage());
    }


    private void logClassLoaders(final ClassLoader pClassLoader) {
        if (pClassLoader != null) {
            ClassLoader currentLoader = pClassLoader;
            while (currentLoader != null) {
                if (currentLoader instanceof URLClassLoader) {
                    LOG.debug("+ URLClassLoader: " + currentLoader.getClass().getName());
                    final URLClassLoader urlLoader = (URLClassLoader) currentLoader;
                    for (final URL url : urlLoader.getURLs()) {
                        LOG.debug(" + URL: " + url);
                    }
                } else {
                    LOG.debug("+ ClassLoader: " + currentLoader.getClass().getName());
                }

                currentLoader = currentLoader.getParent();
            }
        }
    }

    private void logProperties(final ListPropertyResolver resolver) {
        if (resolver != null) {
            final Map<String, String> propertiesToValues = resolver.getPropertyNamesToValues();
            for (final Map.Entry<String, String> propertyEntry : propertiesToValues.entrySet()) {
                final String propertyValue = propertyEntry.getValue();
                LOG.debug("- Property: " + propertyEntry.getKey() + "=" + propertyValue);
            }
        }
    }
}
