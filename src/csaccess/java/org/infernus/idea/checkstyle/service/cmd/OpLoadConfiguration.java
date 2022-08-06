package org.infernus.idea.checkstyle.service.cmd;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.PropertyResolver;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.exception.CheckstyleServiceException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.service.IgnoringResolver;
import org.infernus.idea.checkstyle.service.RulesContainer;
import org.infernus.idea.checkstyle.service.RulesContainer.ConfigurationLocationRulesContainer;
import org.infernus.idea.checkstyle.service.RulesContainer.ContentRulesContainer;
import org.infernus.idea.checkstyle.service.RulesContainer.VirtualFileRulesContainer;
import org.infernus.idea.checkstyle.service.SimpleResolver;
import org.infernus.idea.checkstyle.service.entities.CsConfigObject;
import org.infernus.idea.checkstyle.service.entities.HasCsConfig;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.infernus.idea.checkstyle.CheckStyleBundle.message;
import static org.infernus.idea.checkstyle.util.Notifications.showError;
import static org.infernus.idea.checkstyle.util.Notifications.showWarning;
import static org.infernus.idea.checkstyle.util.Strings.isBlank;


/**
 * Load a Checkstyle configuration file.
 */
public class OpLoadConfiguration
        implements CheckstyleCommand<HasCsConfig> {

    private static final Logger LOG = Logger.getInstance(OpLoadConfiguration.class);

    private static final String TREE_WALKER_ELEMENT = "TreeWalker";
    private static final Map<String, String> FILENAME_REPLACEMENTS = buildReplacementsMap();

    @FunctionalInterface
    private interface ConfigurationLoaderWrapper {
        Configuration loadConfiguration(InputStream inputStream)
                throws NoSuchMethodException, IllegalAccessException, InvocationTargetException;
    }

    private final List<ConfigurationLoaderWrapper> loaderFunctions = Arrays.asList(
            this::loadConfigurationForCheckstylePre825,
            this::loadConfigurationForBrokenCheckstyles,
            this::loadConfigurationForCheckstyle825AndAbove);

    private final RulesContainer rulesContainer;
    private final PropertyResolver resolver;
    private final Module module;
    private final CheckstyleProjectService checkstyleProjectService;


    public OpLoadConfiguration(@NotNull final ConfigurationLocation configurationLocation,
                               @NotNull final CheckstyleProjectService checkstyleProjectService) {
        this(configurationLocation, null, null, checkstyleProjectService);
    }

    public OpLoadConfiguration(@NotNull final ConfigurationLocation configurationLocation,
                               final Map<String, String> properties,
                               @NotNull final CheckstyleProjectService checkstyleProjectService) {
        this(configurationLocation, properties, null, checkstyleProjectService);
    }

    public OpLoadConfiguration(final ConfigurationLocation configurationLocation,
                               final Map<String, String> properties,
                               final Module module,
                               @NotNull final CheckstyleProjectService checkstyleProjectService) {
        this(new ConfigurationLocationRulesContainer(configurationLocation), properties, module, checkstyleProjectService);
    }

    public OpLoadConfiguration(@NotNull final VirtualFile rulesFile,
                               @NotNull final CheckstyleProjectService checkstyleProjectService) {
        this(rulesFile, null, checkstyleProjectService);
    }

    public OpLoadConfiguration(@NotNull final VirtualFile rulesFile,
                               final Map<String, String> properties,
                               @NotNull final CheckstyleProjectService checkstyleProjectService) {
        this(new VirtualFileRulesContainer(rulesFile), properties, null, checkstyleProjectService);
    }

    public OpLoadConfiguration(@NotNull final String fileContent,
                               @NotNull final CheckstyleProjectService checkstyleProjectService) {
        this(new ContentRulesContainer(fileContent), null, null, checkstyleProjectService);
    }

    private OpLoadConfiguration(final RulesContainer rulesContainer,
                                final Map<String, String> properties,
                                final Module module,
                                final CheckstyleProjectService checkstyleProjectService) {
        this.rulesContainer = rulesContainer;
        this.module = module;
        this.checkstyleProjectService = checkstyleProjectService;

        if (properties != null) {
            resolver = new SimpleResolver(properties);
        } else {
            resolver = new IgnoringResolver();
        }
    }

    private static Map<String, String> buildReplacementsMap() {
        return Map.of("RegexpHeader", "headerFile",
                "com.puppycrawl.tools.checkstyle.checks.RegexpHeaderCheck", "headerFile",
                "Header", "headerFile",
                "com.puppycrawl.tools.checkstyle.checks.header.HeaderCheck", "headerFile",
                "SuppressionFilter", "file",
                "com.puppycrawl.tools.checkstyle.filters.SuppressionFilter", "file",
                "SuppressionXpathFilter", "file",
                "com.puppycrawl.tools.checkstyle.filters.SuppressionXpathFilter", "file",
                "ImportControl", "file",
                "com.puppycrawl.tools.checkstyle.checks.imports.ImportControlCheck", "file");
    }


    @Override
    public HasCsConfig execute(@NotNull final Project currentProject) throws CheckstyleException {
        try (InputStream is = rulesContainer.inputStream(checkstyleClassLoader())) {
            Configuration configuration = callLoadConfiguration(is);
            if (configuration == null) {
                // from the CS code this state appears to occur when there's no <module> element found
                // in the input stream
                throw new CheckstyleException("Couldn't find root module in " + rulesContainer.filePath());
            }
            resolveFilePaths(currentProject, configuration);
            return new CsConfigObject(configuration);

        } catch (IOException e) {
            throw new CheckstyleException("Error loading file", e);
        }
    }

    Configuration callLoadConfiguration(final InputStream inputStream) {
        for (ConfigurationLoaderWrapper loaderFunction : loaderFunctions) {
            try {
                return loaderFunction.loadConfiguration(inputStream);

            } catch (NoSuchMethodException ignored) {
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new CheckstyleServiceException("internal error - Failed to call "
                        + ConfigurationLoader.class.getName() + ".loadConfiguration()", e);
            }
        }
        throw new CheckstyleServiceException("internal error - Could not call "
                + ConfigurationLoader.class.getName() + ".loadConfiguration() "
                + "because the method was not found. New Checkstyle runtime?");
    }

    private Configuration loadConfigurationForCheckstylePre825(final InputStream inputStream)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method method = ConfigurationLoader.class.getMethod("loadConfiguration",
                InputSource.class, PropertyResolver.class, boolean.class);
        return (Configuration) method.invoke(null, new InputSource(inputStream), resolver, false);
    }

    private Configuration loadConfigurationForBrokenCheckstyles(final InputStream inputStream)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        // The InputSource method was removed for Checkstyle 6.10, 6.10.1, 6.11, and 6.11.1; and restored in 6.11.2
        Method method = ConfigurationLoader.class.getMethod("loadConfiguration",
                InputStream.class, PropertyResolver.class, boolean.class);
        return (Configuration) method.invoke(null, inputStream, resolver, false);
    }

    private Configuration loadConfigurationForCheckstyle825AndAbove(final InputStream inputStream)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Method method = ConfigurationLoader.class.getMethod("loadConfiguration",
                InputSource.class, PropertyResolver.class, ConfigurationLoader.IgnoredModulesOptions.class);
        return (Configuration) method.invoke(null, new InputSource(inputStream), resolver,
                ConfigurationLoader.IgnoredModulesOptions.EXECUTE);
    }


    void resolveFilePaths(final Project project, @NotNull final Configuration rootElement) {
        if (!(rootElement instanceof DefaultConfiguration)) {
            LOG.warn("Root element is of unknown class: " + rootElement.getClass().getName());
            return;
        }

        for (final Configuration currentChild : rootElement.getChildren()) {
            if (FILENAME_REPLACEMENTS.containsKey(currentChild.getName())) {
                checkFilenameForProperty(project, (DefaultConfiguration) rootElement, currentChild,
                        FILENAME_REPLACEMENTS.get(currentChild.getName()));
            } else if (TREE_WALKER_ELEMENT.equals(currentChild.getName())) {
                resolveFilePaths(project, currentChild);
            }
        }
    }


    private void checkFilenameForProperty(final Project project,
                                          final DefaultConfiguration configRoot,
                                          final Configuration configModule,
                                          final String propertyName) {
        final String fileName = getAttributeOrNull(configModule, propertyName);
        if (!isBlank(fileName)) {
            try {
                resolveAndUpdateFile(project, configRoot, configModule, propertyName, fileName);
            } catch (IOException e) {
                showError(project, message("checkstyle.checker-failed", e.getMessage()));
            }
        }
    }

    private void resolveAndUpdateFile(final Project project,
                                      final DefaultConfiguration configRoot,
                                      final Configuration configModule,
                                      final String propertyName,
                                      final String fileName) throws IOException {
        final String resolvedFile = rulesContainer.resolveAssociatedFile(fileName, module, checkstyleClassLoader());
        if (resolvedFile == null || !resolvedFile.equals(fileName)) {
            configRoot.removeChild(configModule);
            if (resolvedFile != null) {
                configRoot.addChild(elementWithUpdatedFile(resolvedFile, configModule, propertyName));
            } else if (isNotOptional(configModule)) {
                showWarning(project, message(format("checkstyle.not-found.%s", configModule.getName())));
            }
        }
    }

    @NotNull
    private ClassLoader checkstyleClassLoader() {
        return checkstyleProjectService.underlyingClassLoader();
    }

    private boolean isNotOptional(final Configuration configModule) {
        return !"true".equalsIgnoreCase(getAttributeOrNull(configModule, "optional"));
    }

    private String getAttributeOrNull(final Configuration element, final String attributeName) {
        try {
            return element.getAttribute(attributeName);
        } catch (CheckstyleException e) {
            return null;
        }
    }

    private DefaultConfiguration elementWithUpdatedFile(@NotNull final String filename,
                                                        @NotNull final Configuration
                                                                originalElement, @NotNull final String propertyName) {
        // The CheckStyle API won't allow attribute values to be changed, only appended to,
        // hence we must recreate the node.

        final DefaultConfiguration target = new DefaultConfiguration(originalElement.getName());

        copyChildren(originalElement, target);
        copyMessages(originalElement, target);
        copyAttributes(originalElement, propertyName, target);

        target.addAttribute(propertyName, filename);

        return target;
    }

    private void copyAttributes(@NotNull final Configuration source,
                                @NotNull final String propertyName,
                                @NotNull final DefaultConfiguration target) {
        if (source.getAttributeNames() != null) {
            for (String attributeName : source.getAttributeNames()) {
                if (attributeName.equals(propertyName)) {
                    continue;
                }
                target.addAttribute(attributeName, getAttributeOrNull(source, attributeName));
            }
        }
    }

    private void copyMessages(@NotNull final Configuration source,
                              @NotNull final DefaultConfiguration target) {
        final Map<String, String> messages = CheckstyleBridge.messagesFrom(source);
        if (messages != null) {
            for (String messageKey : messages.keySet()) {
                target.addMessage(messageKey, messages.get(messageKey));
            }
        }
    }

    private void copyChildren(@NotNull final Configuration source,
                              @NotNull final DefaultConfiguration target) {
        if (source.getChildren() != null) {
            for (Configuration child : source.getChildren()) {
                target.addChild(child);
            }
        }
    }
}
