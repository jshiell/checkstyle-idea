package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.module.Module;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.infernus.idea.checkstyle.CheckStyleBundle.message;
import static org.infernus.idea.checkstyle.util.Notifications.showError;
import static org.infernus.idea.checkstyle.util.Notifications.showWarning;

public class Configurations {
    private static final Log LOG = LogFactory.getLog(Configurations.class);

    private static final String TREE_WALKER_ELEMENT = "TreeWalker";
    private static final String SUPPRESSION_FILTER_ELEMENT = "SuppressionFilter";
    private static final String SUPPRESSION_FILTER_FILE = "file";
    private static final String IMPORT_CONTROL_ELEMENT = "ImportControl";
    private static final String IMPORT_CONTROL_FILE = "file";
    private static final String REGEXP_HEADER_ELEMENT = "RegexpHeader";
    private static final String REGEXP_HEADER_HEADERFILE = "headerFile";
    private static final String PROPERTY_ELEMENT = "property";
    private static final String DEFAULT_ATTRIBUTE = "default";

    private static final int DEFAULT_TAB_WIDTH = 8;

    private final ConfigurationLocation location;
    private final Module module;

    public Configurations(@NotNull final ConfigurationLocation location,
                          @Nullable final Module module) {
        this.location = location;
        this.module = module;
    }

    public int tabWidth(final Configuration rootElement) {
        for (final Configuration currentChild : rootElement.getChildren()) {
            if (TREE_WALKER_ELEMENT.equals(currentChild.getName())) {
                for (Configuration configuration : currentChild.getChildren()) {
                    if (PROPERTY_ELEMENT.equals(configuration.getName())
                            && "tabWidth".equals(getAttributeOrNull(configuration, "name"))) {
                        return intValueOrDefault(getAttributeOrNull(configuration, "value"), DEFAULT_TAB_WIDTH);
                    }
                }
            }
        }
        return DEFAULT_TAB_WIDTH;
    }

    private int intValueOrDefault(final String value, final int defaultValue) {
        if (value != null) {
            try {
                return Integer.valueOf(value);
            } catch (NumberFormatException ignored) {
                // ignored
            }
        }
        return defaultValue;
    }

    public Configuration resolveFilePaths(@NotNull final Configuration rootElement)
            throws CheckstyleException {

        if (!(rootElement instanceof DefaultConfiguration)) {
            LOG.warn("Root element is of unknown class: " + rootElement.getClass().getName());
            return rootElement;
        }

        for (final Configuration currentChild : rootElement.getChildren()) {
            if (SUPPRESSION_FILTER_ELEMENT.equals(currentChild.getName())) {
                checkFilenameForProperty((DefaultConfiguration) rootElement,
                        currentChild, SUPPRESSION_FILTER_FILE);

            } else if (REGEXP_HEADER_ELEMENT.equals(currentChild.getName())) {
                checkFilenameForProperty((DefaultConfiguration) rootElement,
                        currentChild, REGEXP_HEADER_HEADERFILE);

            } else if (IMPORT_CONTROL_ELEMENT.equals(currentChild.getName())) {
                checkFilenameForProperty((DefaultConfiguration) rootElement,
                        currentChild, IMPORT_CONTROL_FILE);

            } else if (TREE_WALKER_ELEMENT.equals(currentChild.getName())) {
                resolveFilePaths(currentChild);
            }
        }

        return rootElement;
    }

    private void checkFilenameForProperty(final DefaultConfiguration configRoot,
                                          final Configuration configModule,
                                          final String propertyName)
            throws CheckstyleException {
        final String fileName = getAttributeOrNull(configModule, propertyName);
        if (!isEmpty(fileName)) {
            try {
                resolveAndUpdateFile(configRoot, configModule, propertyName, fileName);

            } catch (IOException e) {
                if (module != null) {
                    showError(module.getProject(), message("checkstyle.checker-failed", e.getMessage()));
                }
            }
        }
    }

    private void resolveAndUpdateFile(final DefaultConfiguration configRoot,
                                      final Configuration configModule,
                                      final String propertyName,
                                      final String fileName) throws IOException {
        final String resolvedFile = location.resolveAssociatedFile(fileName, module);
        if (resolvedFile == null || !resolvedFile.equals(fileName)) {
            final String resolvedDefault = resolveDefaultIfPresent(configModule);

            configRoot.removeChild(configModule);
            if (resolvedFile != null || resolvedDefault != null) {
                configRoot.addChild(elementWithUpdatedFile(
                        resolvedFile, resolvedDefault, configModule, propertyName));

            } else if (module != null) {
                showWarning(module.getProject(), message(format("checkstyle.not-found.%s", configModule.getName())));
            }
        }
    }

    @Nullable
    private String resolveDefaultIfPresent(final Configuration element) throws IOException {
        if (asList(element.getAttributeNames()).contains(DEFAULT_ATTRIBUTE)) {
            return location.resolveAssociatedFile(getAttributeOrNull(element, DEFAULT_ATTRIBUTE), module);
        }
        return null;
    }

    private String getAttributeOrNull(final Configuration element, final String attributeName) {
        try {
            return element.getAttribute(attributeName);
        } catch (CheckstyleException e) {
            return null;
        }
    }

    private boolean isEmpty(final String stringValue) {
        return stringValue == null || stringValue.trim().length() == 0;
    }

    private DefaultConfiguration elementWithUpdatedFile(@Nullable final String filename,
                                                        @Nullable final String defaultValue,
                                                        @NotNull final Configuration originalElement,
                                                        @NotNull final String propertyName) {
        // The CheckStyle API won't allow attribute values to be changed, only appended to,
        // hence we must recreate the node.

        final DefaultConfiguration target = new DefaultConfiguration(originalElement.getName());

        copyChildren(originalElement, target);
        copyMessages(originalElement, target);
        copyAttributes(originalElement, propertyName, target);

        if (filename != null) {
            target.addAttribute(propertyName, filename);
        } else {
            target.addAttribute(propertyName, getAttributeOrNull(originalElement, propertyName));
        }
        if (defaultValue != null) {
            target.addAttribute(DEFAULT_ATTRIBUTE, defaultValue);
        }

        return target;
    }

    private void copyAttributes(@NotNull final Configuration source,
                                @NotNull final String propertyName,
                                @NotNull final DefaultConfiguration target) {
        if (source.getAttributeNames() != null) {
            for (String attributeName : source.getAttributeNames()) {
                if (attributeName.equals(propertyName) || attributeName.equals(DEFAULT_ATTRIBUTE)) {
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
