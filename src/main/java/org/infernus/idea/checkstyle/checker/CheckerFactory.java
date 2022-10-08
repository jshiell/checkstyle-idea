package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.exception.CheckstyleToolException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.util.ClassLoaderDumper;
import org.infernus.idea.checkstyle.util.Notifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.infernus.idea.checkstyle.CheckStyleBundle.message;
import static org.infernus.idea.checkstyle.util.Exceptions.rootCauseOf;
import static org.infernus.idea.checkstyle.util.Strings.isBlank;


/**
 * Creates Checkers.
 */
public class CheckerFactory {
    private static final Logger LOG = Logger.getInstance(CheckerFactory.class);

    private final Project project;
    private final CheckstyleProjectService checkstyleProjectService;
    private final CheckerFactoryCache cache;

    @SuppressWarnings("unused") // IDEA's DI
    public CheckerFactory(@NotNull final Project project) {
        this.project = project;
        this.checkstyleProjectService = project.getService(CheckstyleProjectService.class);
        this.cache = project.getService(CheckerFactoryCache.class);
    }

    private CheckerFactory(@NotNull final Project project,
                           @NotNull final CheckstyleProjectService checkstyleProjectService,
                           @NotNull final CheckerFactoryCache cache) {
        this.project = project;
        this.checkstyleProjectService = checkstyleProjectService;
        this.cache = cache;
    }

    public static CheckerFactory create(@NotNull final Project project,
                         @NotNull final CheckstyleProjectService checkstyleProjectService,
                         @NotNull final CheckerFactoryCache cache) {
        return new CheckerFactory(project, checkstyleProjectService, cache);
    }

    public void verify(final ConfigurationLocation location) {
        checker(null, location);
    }

    public Optional<CheckStyleChecker> checker(@Nullable final Module module,
                                               @NotNull final ConfigurationLocation location) {
        LOG.debug("Getting CheckStyle checker with location ", location);

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


    private CachedChecker getOrCreateCachedChecker(@NotNull final ConfigurationLocation location,
                                                   @Nullable final Module module) {
        final Optional<CachedChecker> cachedChecker = cache.get(location, module);
        if (cachedChecker.isPresent()) {
            return cachedChecker.get();
        }

        LOG.debug("No cached checker found, creating a new one for ", location);
        final CachedChecker checker = createChecker(location, module);
        if (checker != null) {
            cache.put(location, module, checker);
            return checker;
        }

        return null;
    }

    private Map<String, String> addEclipseCsProperties(final ConfigurationLocation location,
                                                       final Module module,
                                                       final Map<String, String> properties) {
        addIfAbsent("basedir", basePathFor(module), properties);

        addIfAbsent("project_loc", project.getBasePath(), properties);
        addIfAbsent("workspace_loc", project.getBasePath(), properties);

        final String locationBaseDir = Optional.ofNullable(location.getBaseDir())
                .map(File::toString)
                .orElseGet(project::getBasePath);
        addIfAbsent("config_loc", locationBaseDir, properties);
        addIfAbsent("samedir", locationBaseDir, properties);

        return properties;
    }

    private String basePathFor(final Module module) {
        if (module != null) {
            VirtualFile moduleDir = ProjectUtil.guessModuleDir(module);
            if (moduleDir != null) {
                final File moduleDirFile = new File(moduleDir.getPath());
                if (moduleDirFile.exists()) {
                    return moduleDirFile.getAbsolutePath();
                }
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
        return project.getService(ModuleClassPathBuilder.class);
    }

    private CachedChecker createChecker(@NotNull final ConfigurationLocation location,
                                        @Nullable final Module module) {
        final ListPropertyResolver propertyResolver;
        try {
            location.ensurePropertiesAreUpToDate(checkstyleProjectService.underlyingClassLoader());
            final Map<String, String> properties = removeEmptyProperties(location.getProperties());
            propertyResolver = new ListPropertyResolver(addEclipseCsProperties(location, module, properties));
        } catch (IOException e) {
            LOG.info("CheckStyle properties could not be loaded: " + location.getLocation(), e);
            return blockAndShowMessage(location, module, e, "checkstyle.file-io-failed", location.getLocation());
        }

        final ClassLoader loaderOfCheckedCode = moduleClassPathBuilder().build(module);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Call to create new checker with properties:\n"
                    + dumpProperties(propertyResolver)
                    + "With plugin classloader:"
                    + ClassLoaderDumper.dumpClassLoader(loaderOfCheckedCode));
        }

        final Object workerResult = executeWorker(location, module, propertyResolver, loaderOfCheckedCode);

        if (workerResult instanceof CheckstyleToolException) {
            return blockAndShowMessageFromException(location, module, (CheckstyleToolException) workerResult);
        } else if (workerResult instanceof IOException) {
            IOException ioExceptionResult = (IOException) workerResult;
            LOG.info("CheckStyle configuration could not be loaded: " + location.getLocation(), ioExceptionResult);
            return blockAndShowMessage(location, module, ioExceptionResult, "checkstyle.file-not-found", location.getLocation());
        } else if (workerResult instanceof Throwable) {
            return blockAndShowException(location, module, (Throwable) workerResult);
        }

        return (CachedChecker) workerResult;
    }

    private Map<String, String> removeEmptyProperties(final Map<String, String> properties) {
        Map<String, String> cleanedProperties = new HashMap<>();
        for (Map.Entry<String, String> property : properties.entrySet()) {
            if (!isBlank(property.getValue())) {
                cleanedProperties.put(property.getKey(), property.getValue());
            }
        }
        return cleanedProperties;
    }


    private Object executeWorker(@NotNull final ConfigurationLocation location,
                                 @Nullable final Module module,
                                 final ListPropertyResolver resolver,
                                 @NotNull final ClassLoader loaderOfCheckedCode) {
        final CheckerFactoryWorker worker = new CheckerFactoryWorker(location,
                resolver.getPropertyNamesToValues(), project, module, checkstyleProjectService, loaderOfCheckedCode);
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

    private CachedChecker blockAndShowMessage(final ConfigurationLocation location,
                                              final Module module,
                                              final Throwable cause,
                                              final String messageKey,
                                              final Object... messageArgs) {
        return blockAnd(location, () -> {
            if (module != null) {
                Notifications.showError(module.getProject(), message(messageKey, messageArgs));
            } else {
                throw new CheckStylePluginException(message(messageKey, messageArgs), cause);
            }
        });
    }

    private CachedChecker blockAndShowException(final ConfigurationLocation location,
                                                final Module module,
                                                final Throwable t) {
        return blockAnd(location, () -> {
            if (module != null) {
                Notifications.showException(module.getProject(), t);
            } else if (t instanceof CheckStylePluginException) {
                throw (CheckStylePluginException) t;
            } else {
                throw new CheckStylePluginException(message("checkstyle.parse-failed", rootCauseOf(t).getMessage()), t);
            }
        });
    }

    @Nullable
    private CachedChecker blockAnd(final ConfigurationLocation location,
                                   final Runnable ifNotBlocked) {
        if (!location.isBlocked()) {
            location.block();
            ifNotBlocked.run();
        }
        return null;
    }

    private CachedChecker blockAndShowMessageFromException(final ConfigurationLocation location,
                                                           final Module module,
                                                           final CheckstyleToolException checkstyleException) {
        if (checkstyleException.getMessage().contains("Unable to instantiate DoubleCheckedLocking")) {
            return blockAndShowMessage(location, module, checkstyleException, "checkstyle.double-checked-locking");
        } else if ((checkstyleException.getMessage().contains("unable to parse configuration stream") || checkstyleException.getMessage().contains("Error loading file"))
                && checkstyleException.getCause() != null) {
            return blockAndShowMessage(location, module, checkstyleException.getCause(),
                    "checkstyle.parse-failed", rootCauseOf(checkstyleException).getMessage());
        }

        return blockAndShowMessage(location, module, checkstyleException.getCause(),
                "checkstyle.parse-failed", checkstyleException.getMessage());
    }

    private String dumpProperties(final ListPropertyResolver resolver) {
        StringBuilder dump = new StringBuilder();
        if (resolver != null) {
            final Map<String, String> propertiesToValues = resolver.getPropertyNamesToValues();
            for (final Map.Entry<String, String> propertyEntry : propertiesToValues.entrySet()) {
                dump.append("- Property: ")
                        .append(propertyEntry.getKey())
                        .append("=")
                        .append(propertyEntry.getValue())
                        .append('\n');
            }
        }
        return dump.toString();
    }
}
