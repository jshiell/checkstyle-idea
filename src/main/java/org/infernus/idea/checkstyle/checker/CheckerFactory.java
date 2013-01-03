package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.PropertyResolver;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.util.IDEAUtilities;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A configuration factory and resolver for CheckStyle.
 */
public class CheckerFactory {

    private static final Log LOG = LogFactory.getLog(CheckerFactory.class);

    private final Map<ConfigurationLocation, CachedChecker> cache = new HashMap<ConfigurationLocation, CachedChecker>();

    /**
     * Get a checker for a given configuration.
     *
     * @param location    the location of the CheckStyle file.
     * @param module      the current module.
     * @param classLoader class loader for CheckStyle use, or null to use
     *                    the default.
     * @return the checker for the module or null if it cannot be created.
     * @throws IOException         if the CheckStyle file cannot be resolved.
     * @throws CheckstyleException if CheckStyle initialisation fails.
     */
    public Checker getChecker(final ConfigurationLocation location,
                              final Module module,
                              final ClassLoader classLoader)
            throws CheckstyleException, IOException {
        if (location == null) {
            throw new IllegalArgumentException("Location is required");
        }

        synchronized (cache) {
            if (cache.containsKey(location)) {
                final CachedChecker cachedChecker = cache.get(location);
                if (cachedChecker != null && cachedChecker.isValid()) {
                    return cachedChecker.getChecker();
                } else {
                    if (cachedChecker != null) {
                        cachedChecker.getChecker().destroy();
                    }
                    cache.remove(location);
                }
            }

            final ListPropertyResolver propertyResolver = new ListPropertyResolver(location.getProperties());
            final CachedChecker checker = createChecker(location, module, propertyResolver, classLoader);
            if (checker != null) {
                cache.put(location, checker);
                return checker.getChecker();
            }

            return null;
        }
    }

    /**
     * Invalidate any cached checkers.
     */
    public void invalidateCache() {
        synchronized (cache) {
            for (CachedChecker cachedChecker : cache.values()) {
                cachedChecker.getChecker().destroy();
            }
            cache.clear();
        }
    }

    /**
     * Get the checker configuration for a given configuration.
     *
     * @param location the location of the CheckStyle file.
     * @return a configuration.
     */
    public Configuration getConfig(final ConfigurationLocation location) {
        if (location == null) {
            throw new IllegalArgumentException("Location is required");
        }

        synchronized (cache) {
            if (cache.containsKey(location)) {
                CachedChecker cachedChecker = cache.get(location);
                if (cachedChecker != null && cachedChecker.isValid()) {
                    return cachedChecker.getConfig();
                }
            }
        }

        throw new IllegalArgumentException("Failed to find a configured checker.");
    }

    /**
     * Load the Checkstyle configuration in a separate thread.
     *
     * @param location           The location of the Checkstyle configuration file.
     * @param module             the current module.
     * @param resolver           the resolver.
     * @param contextClassLoader the context class loader, or null for default.
     * @return loaded Configuration object
     * @throws CheckstyleException If there was any error loading the configuration file.
     */
    private CachedChecker createChecker(final ConfigurationLocation location,
                                        final Module module,
                                        final PropertyResolver resolver,
                                        final ClassLoader contextClassLoader)
            throws CheckstyleException {

        if (LOG.isDebugEnabled()) {
            // debug information

            LOG.debug("Call to create new checker.");

            logProperties(resolver);
            logClassLoaders(contextClassLoader);
        }

        final CheckerFactoryWorker worker = new CheckerFactoryWorker(
                location, resolver, module, contextClassLoader);

        // Begin reading the configuration
        worker.start();

        // Wait for configuration thread to complete
        while (worker.isAlive()) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                // Just be silent for now
            }
        }

        // Did the process of reading the configuration fail?
        if (worker.getResult() instanceof CheckstyleException) {
            throw (CheckstyleException) worker.getResult();

        } else if (worker.getResult() instanceof IOException) {
            LOG.info("CheckStyle configuration could not be loaded: " + location.getLocation(),
                    (IOException) worker.getResult());

            final MessageFormat notFoundFormat = new MessageFormat(
                    IDEAUtilities.getResource("checkstyle.file-not-found", "Not found: {0}"));
            IDEAUtilities.showError(module.getProject(), notFoundFormat.format(new Object[]{location.getLocation()}));
            return null;

        } else if (worker.getResult() instanceof Throwable) {
            throw new CheckstyleException("Could not load configuration",
                    (Throwable) worker.getResult());
        }

        return (CachedChecker) worker.getResult();
    }

    private void logClassLoaders(final ClassLoader contextClassLoader) {
        // Log classloaders, if known
        if (contextClassLoader != null) {
            ClassLoader currentLoader = contextClassLoader;
            while (currentLoader != null) {
                if (currentLoader instanceof URLClassLoader) {
                    LOG.debug("+ URLClassLoader: "
                            + currentLoader.getClass().getName());
                    final URLClassLoader urlLoader = (URLClassLoader)
                            currentLoader;
                    for (final URL url : urlLoader.getURLs()) {
                        LOG.debug(" + URL: " + url);
                    }
                } else {
                    LOG.debug("+ ClassLoader: "
                            + currentLoader.getClass().getName());
                }

                currentLoader = currentLoader.getParent();
            }
        }
    }

    private void logProperties(final PropertyResolver resolver) {
        // Log properties if known
        if (resolver != null && resolver instanceof ListPropertyResolver) {
            final ListPropertyResolver listResolver = (ListPropertyResolver)
                    resolver;
            final Map<String, String> propertiesToValues
                    = listResolver.getPropertyNamesToValues();
            for (final String propertyName : propertiesToValues.keySet()) {
                final String propertyValue
                        = propertiesToValues.get(propertyName);
                LOG.debug("- Property: " + propertyName + "="
                        + propertyValue);
            }
        }
    }

    private class CheckerFactoryWorker extends Thread {
        private static final String SUPPRESSION_FILTER_ELEMENT = "SuppressionFilter";
        private static final String SUPPRESSION_FILTER_FILE = "file";
        private static final String REGEXP_HEADER_ELEMENT = "RegexpHeader";
        private static final String REGEXP_HEADER_HEADERFILE = "headerFile";

        private final Object[] threadReturn = new Object[1];

        private final ConfigurationLocation location;
        private final PropertyResolver resolver;
        private final Module module;

        public CheckerFactoryWorker(final ConfigurationLocation location,
                                    final PropertyResolver resolver,
                                    final Module module,
                                    final ClassLoader contextClassLoader) {
            this.location = location;
            this.resolver = resolver;
            this.module = module;


            if (contextClassLoader != null) {
                setContextClassLoader(contextClassLoader);
            } else {
                final ClassLoader loader = CheckerFactory.this.getClass().getClassLoader();
                setContextClassLoader(loader);
            }
        }

        public Object getResult() {
            return threadReturn[0];
        }

        public void run() {
            try {
                final Checker checker = new Checker();
                final Configuration config;

                if (location != null) {
                    InputStream configurationInputStream = null;

                    try {
                        configurationInputStream = location.resolve();
                        config = ConfigurationLoader.loadConfiguration(
                                new InputSource(configurationInputStream), resolver, true);

                        replaceSuppressionFilterPath(config);

                        checker.setModuleClassLoader(Thread.currentThread().getContextClassLoader());
                        checker.configure(config);

                    } finally {
                        if (configurationInputStream != null) {
                            try {
                                configurationInputStream.close();
                            } catch (IOException e) {
                                // ignored
                            }
                        }
                    }
                } else {
                    config = new DefaultConfiguration("checker");
                }
                threadReturn[0] = new CachedChecker(checker, config);

            } catch (Exception e) {
                threadReturn[0] = e;
            }
        }

        /**
         * Scans the configuration for suppression filters and
         * replaces relative paths with absolute ones.
         *
         * @param rootElement the current configuration.
         * @throws CheckstyleException if configuration fails.
         */
        private void replaceSuppressionFilterPath(final Configuration rootElement)
                throws CheckstyleException {

            if (!(rootElement instanceof DefaultConfiguration)) {
                LOG.warn("Root element is of unknown class: " + rootElement.getClass().getName());
                return;
            }

            for (final Configuration currentChild : rootElement.getChildren()) {
                if (SUPPRESSION_FILTER_ELEMENT.equals(currentChild.getName())) {
                    checkFilenameForProperty((DefaultConfiguration) rootElement,
                            currentChild, SUPPRESSION_FILTER_ELEMENT, SUPPRESSION_FILTER_FILE);

                } else if (REGEXP_HEADER_ELEMENT.equals(currentChild.getName())) {
                    checkFilenameForProperty((DefaultConfiguration) rootElement,
                            currentChild, REGEXP_HEADER_ELEMENT, REGEXP_HEADER_HEADERFILE);
                }
            }
        }

        private void checkFilenameForProperty(final DefaultConfiguration rootElement,
                                              final Configuration currentChild,
                                              final String elementName,
                                              final String propertyName)
                throws CheckstyleException {
            final String[] attributeNames = currentChild.getAttributeNames();
            if (attributeNames == null || Arrays.binarySearch(attributeNames, propertyName) < 0) {
                return;
            }

            final String fileName = currentChild.getAttribute(propertyName);
            if (fileName != null && !new File(fileName).exists()) {
                final File resolvedFile = findFile(fileName);

                rootElement.removeChild(currentChild);

                if (resolvedFile != null) {
                    rootElement.addChild(elementWithUpdatedFile(
                            resolvedFile.getAbsolutePath(), currentChild, elementName, propertyName));

                } else if (module != null) {
                    IDEAUtilities.showWarning(module.getProject(),
                            IDEAUtilities.getResource(String.format("checkstyle.not-found.%s", elementName),
                                    String.format("CheckStyle %s %s not found", elementName, propertyName)));
                }
            }
        }

        private DefaultConfiguration elementWithUpdatedFile(@NotNull final String filename,
                                                            @NotNull final Configuration originalElement,
                                                            @NotNull final String elementName,
                                                            @NotNull final String propertyName) {
            // The CheckStyle API won't allow attribute values to be changed, only appended to,
            // hence we must recreate the node.

            final DefaultConfiguration newFilter = new DefaultConfiguration(elementName);

            if (originalElement.getChildren() != null) {
                for (Configuration child : originalElement.getChildren()) {
                    newFilter.addChild(child);
                }
            }
            if (originalElement.getMessages() != null) {
                for (String messageKey : originalElement.getMessages().keySet()) {
                    newFilter.addMessage(messageKey, originalElement.getMessages().get(messageKey));
                }
            }
            if (originalElement.getAttributeNames() != null) {
                for (String attributeName : originalElement.getAttributeNames()) {
                    if (attributeName.equals(propertyName)) {
                        continue;
                    }
                    try {
                        newFilter.addAttribute(attributeName, originalElement.getAttribute(attributeName));
                    } catch (CheckstyleException e) {
                        LOG.error("Unable to copy attribute for " + elementName + ": " + attributeName, e);
                    }
                }
            }

            newFilter.addAttribute(propertyName, filename);

            return newFilter;
        }

        private File findFile(final String fileName) {
            File suppressionFile = null;

            // check relative to config
            if (location.getBaseDir() != null) {
                final File configFileRelativePath = new File(location.getBaseDir(), fileName);
                if (configFileRelativePath.exists()) {
                    suppressionFile = configFileRelativePath;
                }
            }

            if (module != null) {
                final ModuleRootManager rootManager = ModuleRootManager.getInstance(module);

                // check module content roots                
                if (suppressionFile == null && rootManager.getContentEntries().length > 0) {
                    for (final ContentEntry contentEntry : rootManager.getContentEntries()) {
                        final File contentEntryPath = new File(contentEntry.getFile().getPath(), fileName);
                        if (contentEntryPath.exists()) {
                            suppressionFile = contentEntryPath;
                            break;
                        }
                    }
                }

                // check module file
                if (suppressionFile == null && module.getModuleFile() != null) {
                    final File moduleRelativePath = new File(module.getModuleFile().getParent().getPath(), fileName);
                    if (moduleRelativePath.exists()) {
                        suppressionFile = moduleRelativePath;
                    }
                }

                // check project base dir
                if (suppressionFile == null && module.getProject().getBaseDir() != null) {
                    final File projectRelativePath = new File(module.getProject().getBaseDir().getPath(), fileName);
                    if (projectRelativePath.exists()) {
                        suppressionFile = projectRelativePath;
                    }
                }
            }

            return suppressionFile;
        }
    }
}
