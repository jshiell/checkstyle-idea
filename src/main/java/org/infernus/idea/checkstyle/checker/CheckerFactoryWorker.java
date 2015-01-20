package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.module.Module;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.PropertyResolver;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Arrays;

import static java.lang.String.format;
import static org.infernus.idea.checkstyle.util.IDEAUtilities.getResource;
import static org.infernus.idea.checkstyle.util.IDEAUtilities.showError;
import static org.infernus.idea.checkstyle.util.IDEAUtilities.showWarning;

class CheckerFactoryWorker extends Thread {
    private static final Log LOG = LogFactory.getLog(CheckerFactory.class);

    private static final String TREE_WALKER_ELEMENT = "TreeWalker";
    private static final String SUPPRESSION_FILTER_ELEMENT = "SuppressionFilter";
    private static final String SUPPRESSION_FILTER_FILE = "file";
    private static final String IMPORT_CONTROL_ELEMENT = "ImportControl";
    private static final String IMPORT_CONTROL_FILE = "file";
    private static final String REGEXP_HEADER_ELEMENT = "RegexpHeader";
    private static final String REGEXP_HEADER_HEADERFILE = "headerFile";
    private static final String PROPERTY_ELEMENT = "property";
    private static final int DEFAULT_TAB_WIDTH = 8;

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
            setContextClassLoader(getClass().getClassLoader());
        }
    }

    public Object getResult() {
        return threadReturn[0];
    }

    public void run() {
        try {
            int tabWidth = DEFAULT_TAB_WIDTH;
            final Checker checker = new Checker();
            Configuration config = null;

            if (location != null) {
                InputStream configurationInputStream = null;

                try {
                    configurationInputStream = location.resolve();
                    config = ConfigurationLoader.loadConfiguration(
                            new InputSource(configurationInputStream), resolver, true);
                    if (config == null) {
                        // from the CS code this state appears to occur when there's no <module> element found
                        // in the input stream
                        throw new CheckstyleException("Couldn't find root module in " + location.getLocation());
                    }

                    replaceFilePaths(config);
                    tabWidth = findTabWidthFrom(config);
                    checker.setModuleClassLoader(Thread.currentThread().getContextClassLoader());
                    checker.configure(config);

                } finally {
                    closeQuietly(configurationInputStream);
                }
            }

            if (config == null) {
                config = new DefaultConfiguration("checker");
            }
            threadReturn[0] = new CachedChecker(new CheckerContainer(checker, tabWidth), config);

        } catch (Exception e) {
            threadReturn[0] = e;
        }
    }

    private void closeQuietly(final InputStream configurationInputStream) {
        if (configurationInputStream != null) {
            try {
                configurationInputStream.close();
            } catch (Exception ignored) {
                // ignored
            }
        }
    }

    private int findTabWidthFrom(final Configuration rootElement) {
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

    /**
     * Scans the configuration for elements with filenames and
     * replaces relative paths with absolute ones.
     *
     * @param rootElement the current configuration.
     * @throws com.puppycrawl.tools.checkstyle.api.CheckstyleException if configuration fails.
     */
    private void replaceFilePaths(final Configuration rootElement)
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

            } else if (IMPORT_CONTROL_ELEMENT.equals(currentChild.getName())) {
                checkFilenameForProperty((DefaultConfiguration) rootElement,
                        currentChild, IMPORT_CONTROL_ELEMENT, IMPORT_CONTROL_FILE);

            } else if (TREE_WALKER_ELEMENT.equals(currentChild.getName())) {
                replaceFilePaths(currentChild);
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
        try {
            final String resolvedFile = location.resolveAssociatedFile(fileName, module);
            if (!isEmpty(fileName)
                    && (resolvedFile == null || !resolvedFile.equals(fileName))) {
                rootElement.removeChild(currentChild);

                if (resolvedFile != null) {
                    rootElement.addChild(elementWithUpdatedFile(
                            resolvedFile, currentChild, elementName, propertyName));

                } else if (module != null) {
                    showWarning(module.getProject(), getResource(
                            format("checkstyle.not-found.%s", elementName),
                            format("CheckStyle %s %s not found", elementName, propertyName)));
                }
            }

        } catch (IOException e) {
            if (module != null) {
                final MessageFormat errorFormat = new MessageFormat(
                        getResource("checkstyle.checker-failed", "Load failed due to {0}"));
                showError(module.getProject(), errorFormat.format(e.getMessage()));
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
