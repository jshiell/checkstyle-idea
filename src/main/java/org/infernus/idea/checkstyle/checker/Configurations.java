package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.module.Module;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;

import static java.lang.String.format;
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
                    try {
                        if (PROPERTY_ELEMENT.equals(configuration.getName())
                                && "tabWidth".equals(configuration.getAttribute("name"))) {
                            return Integer.parseInt(configuration.getAttribute("value"));
                        }
                    } catch (Exception ignored) {
                        // every property is required to have a name and value element
                    }
                }
            }
        }
        return DEFAULT_TAB_WIDTH;
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
                        currentChild, SUPPRESSION_FILTER_ELEMENT, SUPPRESSION_FILTER_FILE);

            } else if (REGEXP_HEADER_ELEMENT.equals(currentChild.getName())) {
                checkFilenameForProperty((DefaultConfiguration) rootElement,
                        currentChild, REGEXP_HEADER_ELEMENT, REGEXP_HEADER_HEADERFILE);

            } else if (IMPORT_CONTROL_ELEMENT.equals(currentChild.getName())) {
                checkFilenameForProperty((DefaultConfiguration) rootElement,
                        currentChild, IMPORT_CONTROL_ELEMENT, IMPORT_CONTROL_FILE);

            } else if (TREE_WALKER_ELEMENT.equals(currentChild.getName())) {
                resolveFilePaths(currentChild);
            }
        }

        return rootElement;
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
        try {
            final String resolvedFile = location.resolveAssociatedFile(fileName, module);
            if (!isEmpty(fileName)
                    && (resolvedFile == null || !resolvedFile.equals(fileName))) {
                rootElement.removeChild(currentChild);

                if (resolvedFile != null) {
                    rootElement.addChild(elementWithUpdatedFile(
                            resolvedFile, currentChild, elementName, propertyName));

                } else if (module != null) {
                    showWarning(module.getProject(), CheckStyleBundle.message(
                            format("checkstyle.not-found.%s", elementName)));
                }
            }

        } catch (IOException e) {
            if (module != null) {
                showError(module.getProject(), CheckStyleBundle.message("checkstyle.checker-failed", e.getMessage()));
            }
        }
    }

    private boolean isEmpty(final String fileName) {
        return fileName == null || fileName.trim().length() == 0;
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

}
