package org.infernus.idea.checkstyle.checker;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.module.Module;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.infernus.idea.checkstyle.CheckStyleBundle.message;
import static org.infernus.idea.checkstyle.util.Notifications.showError;
import static org.infernus.idea.checkstyle.util.Notifications.showWarning;
import static org.infernus.idea.checkstyle.util.Strings.isBlank;

public class Configurations {
    private static final Log LOG = LogFactory.getLog(Configurations.class);

    private static final String TREE_WALKER_ELEMENT = "TreeWalker";

    private static final Map<String, String> FILENAME_REPLACEMENTS = new HashMap<String, String>() {{
        put("RegexpHeader", "headerFile");
        put("Header", "headerFile");
        put("SuppressionFilter", "file");
        put("ImportControl", "file");
    }};

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
                return intValueOrDefault(getAttributeOrNull(currentChild, "tabWidth"), defaultTabSize());
            }
        }
        return defaultTabSize();
    }

    private int defaultTabSize() {
        return currentCodeStyleSettings().getTabSize(JavaFileType.INSTANCE);
    }

    @NotNull
    CodeStyleSettings currentCodeStyleSettings() {
        return codeStyleSettingsManager().getCurrentSettings();
    }

    private CodeStyleSettingsManager codeStyleSettingsManager() {
        if (module != null) {
            return CodeStyleSettingsManager.getInstance(module.getProject());
        }
        return CodeStyleSettingsManager.getInstance();
    }

    public Optional<String> baseDir(final Configuration rootElement) {
        for (final String attributeName : rootElement.getAttributeNames()) {
            if ("basedir".equals(attributeName)) {
                return ofNullable(getAttributeOrNull(rootElement, "basedir"));
            }
        }
        return empty();
    }

    private int intValueOrDefault(final String value, final int defaultValue) {
        if (value != null) {
            try {
                return Integer.valueOf(value);
            } catch (NumberFormatException ignored) {
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
            if (FILENAME_REPLACEMENTS.containsKey(currentChild.getName())) {
                checkFilenameForProperty((DefaultConfiguration) rootElement,
                        currentChild, FILENAME_REPLACEMENTS.get(currentChild.getName()));

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
        if (!isBlank(fileName)) {
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
            configRoot.removeChild(configModule);
            if (resolvedFile != null) {
                configRoot.addChild(elementWithUpdatedFile(
                        resolvedFile, configModule, propertyName));

            } else if (module != null && isNotOptional(configModule)) {
                showWarning(module.getProject(), message(format("checkstyle.not-found.%s", configModule.getName())));
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
                                                        @NotNull final Configuration originalElement,
                                                        @NotNull final String propertyName) {
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
