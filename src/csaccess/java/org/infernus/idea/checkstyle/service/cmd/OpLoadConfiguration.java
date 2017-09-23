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
import org.apache.commons.io.IOUtils;
import org.infernus.idea.checkstyle.exception.CheckstyleServiceException;
import org.infernus.idea.checkstyle.model.BundledConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.service.IgnoringResolver;
import org.infernus.idea.checkstyle.service.RulesContainer;
import org.infernus.idea.checkstyle.service.RulesContainer.BundledRulesContainer;
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
import java.util.Collections;
import java.util.HashMap;
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

    private final RulesContainer rulesContainer;
    private final PropertyResolver resolver;
    private final Module module;


    public OpLoadConfiguration(@NotNull final ConfigurationLocation configurationLocation) {
        this(configurationLocation, null, null);
    }

    public OpLoadConfiguration(@NotNull final ConfigurationLocation configurationLocation,
                               final Map<String, String> properties) {
        this(configurationLocation, properties, null);
    }

    public OpLoadConfiguration(final ConfigurationLocation configurationLocation,
                               final Map<String, String> properties,
                               final Module module) {
        this(configurationLocation instanceof BundledConfigurationLocation ?
                new BundledRulesContainer(((BundledConfigurationLocation) configurationLocation).getBundledConfig()) :
                new ConfigurationLocationRulesContainer(configurationLocation), properties, module);
    }

    public OpLoadConfiguration(@NotNull final VirtualFile rulesFile) {
        this(rulesFile, null);
    }

    public OpLoadConfiguration(@NotNull final VirtualFile rulesFile,
                               final Map<String, String> properties) {
        this(new VirtualFileRulesContainer(rulesFile), properties, null);
    }

    public OpLoadConfiguration(@NotNull final String fileContent) {
        this(new ContentRulesContainer(fileContent), null, null);
    }

    private OpLoadConfiguration(final RulesContainer rulesContainer,
                                final Map<String, String> properties,
                                final Module module) {
        this.rulesContainer = rulesContainer;
        this.module = module;

        if (properties != null) {
            resolver = new SimpleResolver(properties);
        } else {
            resolver = new IgnoringResolver();
        }
    }

    private static Map<String, String> buildReplacementsMap() {
        Map<String, String> result = new HashMap<>();
        result.put("RegexpHeader", "headerFile");
        result.put("Header", "headerFile");
        result.put("SuppressionFilter", "file");
        result.put("ImportControl", "file");
        return Collections.unmodifiableMap(result);
    }


    @Override
    public HasCsConfig execute(@NotNull final Project currentProject) throws CheckstyleException {
        HasCsConfig result;
        InputStream is = null;
        try {
            is = rulesContainer.inputStream();
            Configuration configuration = callLoadConfiguration(is);
            if (configuration == null) {
                // from the CS code this state appears to occur when there's no <module> element found
                // in the input stream
                throw new CheckstyleException("Couldn't find root module in " + rulesContainer.filePath());
            }
            resolveFilePaths(currentProject, configuration);
            result = new CsConfigObject(configuration);

        } catch (IOException e) {
            throw new CheckstyleException("Error loading file", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return result;
    }


    Configuration callLoadConfiguration(final InputStream inputStream) {
        boolean inputSourceRequired = false;
        Method method;
        try {
            // This will fail in Checkstyle 6.10, 6.10.1, 6.11, and 6.11.1. The method was re-enabled in 6.11.2.
            method = ConfigurationLoader.class.getMethod("loadConfiguration",
                    InputSource.class, PropertyResolver.class, boolean.class);
            inputSourceRequired = true;
        } catch (NoSuchMethodException e) {
            try {
                // Solution for Checkstyle 6.10, 6.10.1, 6.11, and 6.11.1.
                method = ConfigurationLoader.class.getMethod("loadConfiguration",
                        InputStream.class, PropertyResolver.class, boolean.class);
            } catch (NoSuchMethodException pE) {
                throw new CheckstyleServiceException("internal error - Could not call "
                        + ConfigurationLoader.class.getName() + ".loadConfiguration() "
                        + "because the method was not found. New Checkstyle runtime?");
            }
        }

        Configuration result;
        try {
            if (inputSourceRequired) {
                result = (Configuration) method.invoke(null, new InputSource(inputStream), resolver, false);
            } else {
                result = (Configuration) method.invoke(null, inputStream, resolver, false);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new CheckstyleServiceException("internal error - Failed to call " //
                    + ConfigurationLoader.class.getName() + ".loadConfiguration()", e);
        }
        return result;
    }


    void resolveFilePaths(final Project project, @NotNull final Configuration rootElement) throws CheckstyleException {
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
                                          final String propertyName) throws CheckstyleException {
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
        final String resolvedFile = rulesContainer.resolveAssociatedFile(fileName, project, module);
        if (resolvedFile == null || !resolvedFile.equals(fileName)) {
            configRoot.removeChild(configModule);
            if (resolvedFile != null) {
                configRoot.addChild(elementWithUpdatedFile(resolvedFile, configModule, propertyName));
            } else if (isNotOptional(configModule)) {
                showWarning(project, message(format("checkstyle.not-found.%s", configModule.getName())));
            }
        }
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
        if (source.getMessages() != null) {
            for (String messageKey : source.getMessages().keySet()) {
                target.addMessage(messageKey, source.getMessages().get(messageKey));
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
