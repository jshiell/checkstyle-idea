package org.infernus.idea.checkstyle.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.util.CheckStyleEntityResolver;
import org.infernus.idea.checkstyle.util.Objects;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.lang.System.currentTimeMillis;
import static org.infernus.idea.checkstyle.util.Strings.isBlank;

/**
 * Bean encapsulating a configuration source.
 * <p>Note on identity: Configuration locations are considered equal if their descriptor matches. The descriptor
 * consists of type, location, and description text. Properties are not considered.</p>
 * <p>Note on order: Configuration locations are ordered by description text, followed by location and type, except that
 * the bundled configurations (Sun and Google checks) always go first.</p>
 */
public abstract class ConfigurationLocation implements Cloneable, Comparable<ConfigurationLocation>
{
    private static final Log LOG = LogFactory.getLog(ConfigurationLocation.class);

    private static final long BLACKLIST_TIME_MS = 1000 * 60;

    private final Map<String, String> properties = new HashMap<>();
    private final ConfigurationType type;
    private String location;
    private String description;

    private boolean propertiesCheckedThisSession;
    private long blacklistedUntil;

    public ConfigurationLocation(final ConfigurationType type) {
        if (type == null) {
            throw new IllegalArgumentException("A type is required");
        }

        this.type = type;
    }

    /**
     * Get the base directory for this checkstyle file. If null then the project directory is assumed.
     *
     * @return the base directory for the file, or null if not applicable to the location type.
     */
    public File getBaseDir() {
        return null;
    }

    public ConfigurationType getType() {
        return type;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        if (isBlank(location)) {
            throw new IllegalArgumentException("A non-blank location is required");
        }

        this.location = location;
        if (description == null) {
            description = location;
        }

        this.propertiesCheckedThisSession = false;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable final String description) {
        if (description == null) {
            this.description = location;
        } else {
            this.description = description;
        }
    }

    public Map<String, String> getProperties() throws IOException {
        if (!propertiesCheckedThisSession) {
            resolveFile();
        }

        return Collections.unmodifiableMap(properties);
    }

    public void setProperties(final Map<String, String> newProperties) {
        properties.clear();

        if (newProperties == null) {
            return;
        }

        properties.putAll(newProperties);

        this.propertiesCheckedThisSession = false;
    }

    public boolean isEditableInConfigDialog() {
        return true;
    }

    public void reset() {
        propertiesCheckedThisSession = false;
        blacklistedUntil = 0L;
    }

    /**
     * Extract all settable properties from the given configuration file.
     *
     * @param inputStream the configuration file.
     * @return the property names.
     */
    private List<String> extractProperties(final InputStream inputStream) {
        if (inputStream != null) {
            try {
                final SAXBuilder saxBuilder = new SAXBuilder();
                saxBuilder.setEntityResolver(new CheckStyleEntityResolver());
                final Document configDoc = saxBuilder.build(inputStream);
                return extractProperties(configDoc.getRootElement());

            } catch (Exception e) {
                LOG.warn("CheckStyle file could not be parsed for properties.", e);
            }
        }

        return new ArrayList<>();
    }

    /**
     * Extract all settable properties from the given configuration element.
     *
     * @param element the configuration element.
     * @return the settable property names.
     */
    @SuppressWarnings("unchecked")
    private List<String> extractProperties(final Element element) {
        final List<String> propertyNames = new ArrayList<>();

        if (element != null) {
            extractPropertyNames(element, propertyNames);

            for (final Element child : element.getChildren()) {
                propertyNames.addAll(extractProperties(child));
            }
        }

        return propertyNames;
    }

    private void extractPropertyNames(final Element element, final List<String> propertyNames) {
        if (!"property".equals(element.getName())) {
            return;
        }

        final String value = element.getAttributeValue("value");
        if (value == null) {
            return;
        }

        final int propertyStart = value.indexOf("${");
        final int propertyEnd = value.indexOf('}');
        if (propertyStart >= 0 && propertyEnd >= 0) {
            final String propertyName = value.substring(
                    propertyStart + 2, propertyEnd);
            propertyNames.add(propertyName);
        }
    }

    /**
     * Resolve this location to a file.
     *
     * @return the file to load.
     * @throws IOException if the file cannot be loaded.
     */
    public InputStream resolve() throws IOException {
        InputStream is = resolveFile();

        if (!propertiesCheckedThisSession) {
            final List<String> propertiesInFile = extractProperties(is);

            for (final String propertyName : propertiesInFile) {
                if (!properties.containsKey(propertyName)) {
                    properties.put(propertyName, "");
                }
            }

            properties.keySet().removeIf(propertyName -> !propertiesInFile.contains(propertyName));

            try {
                is.reset();
            } catch (IOException e) {
                is = resolveFile(); // JAR IS doesn't support this, for instance
            }

            propertiesCheckedThisSession = true;
        }

        return is;
    }

    @Nullable
    public String resolveAssociatedFile(final String filename,
                                        final Project project,
                                        final Module module) throws IOException {
        if (filename == null) {
            return null;
        } else if (new File(filename).exists()) {
            return filename;
        }

        return findFile(filename, project, module);
    }

    private String findFile(final String fileName,
                            final Project project,
                            final Module module) {
        if (fileName == null
                || "".equals(fileName.trim())
                || fileName.toLowerCase().startsWith("http://")
                || fileName.toLowerCase().startsWith("https://")) {
            return fileName;
        }

        File targetFile = checkCommonPathsForTarget(fileName, project, module);

        if (targetFile != null) {
            return targetFile.getAbsolutePath();
        }
        return null;
    }

    private File checkCommonPathsForTarget(final String fileName,
                                           final Project project,
                                           final Module module) {
        File targetFile = checkRelativeToRulesFile(fileName);
        if (module != null) {
            if (targetFile == null) {
                targetFile = checkModuleContentRoots(module, fileName);
            }
            if (targetFile == null) {
                targetFile = checkModuleFile(module, fileName);
            }
        }
        if (targetFile == null) {
            targetFile = checkProjectBaseDir(project, fileName);
        }
        return targetFile;
    }

    private File checkRelativeToRulesFile(final String fileName) {
        if (getBaseDir() != null) {
            final File configFileRelativePath = new File(getBaseDir(), fileName);
            if (configFileRelativePath.exists()) {
                return configFileRelativePath;
            }
        }
        return null;
    }

    private File checkProjectBaseDir(final Project project,
                                     final String fileName) {
        if (project.getBaseDir() != null) {
            final File projectRelativePath = new File(project.getBaseDir().getPath(), fileName);
            if (projectRelativePath.exists()) {
                return projectRelativePath;
            }
        }
        return null;
    }

    private File checkModuleFile(final Module module,
                                 final String fileName) {
        if (module.getModuleFile() != null) {
            final File moduleRelativePath = new File(module.getModuleFile().getParent().getPath(), fileName);
            if (moduleRelativePath.exists()) {
                return moduleRelativePath;
            }
        }
        return null;
    }

    private File checkModuleContentRoots(final Module module, final String fileName) {
        ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
        if (rootManager.getContentEntries().length > 0) {
            for (final ContentEntry contentEntry : rootManager.getContentEntries()) {
                if (contentEntry.getFile() == null) {
                    continue;
                }

                final File contentEntryPath = new File(contentEntry.getFile().getPath(), fileName);
                if (contentEntryPath.exists()) {
                    return contentEntryPath;
                }
            }
        }
        return null;
    }

    public final boolean hasChangedFrom(final ConfigurationLocation configurationLocation) throws IOException {
        return configurationLocation == null
                || !equals(configurationLocation)
                || !getProperties().equals(configurationLocation.getProperties());

    }

    public String getDescriptor() {
        assert location != null;
        assert description != null;

        return type + ":" + location + ":" + description;
    }

    /**
     * Resolve this location to a file.
     *
     * @return the file to load.
     * @throws IOException if the file cannot be loaded.
     */
    @NotNull
    protected abstract InputStream resolveFile() throws IOException;

    @Override
    public abstract Object clone();

    ConfigurationLocation cloneCommonPropertiesTo(final ConfigurationLocation cloned) {
        cloned.setDescription(getDescription());
        cloned.setLocation(getLocation());
        try {
            cloned.setProperties(new HashMap<>(getProperties()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to resolve properties for " + this);
        }
        return cloned;
    }


    @Override
    public final boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ConfigurationLocation)) {
            return false;
        }
        final ConfigurationLocation that = (ConfigurationLocation) other;
        return compareTo(that) == 0;
    }

    @Override
    public final int hashCode() {
        int result = java.util.Objects.hash(getDescription(), getLocation(), getType());
        if (this instanceof BundledConfigurationLocation) {
            result = java.util.Objects.hash(result, ((BundledConfigurationLocation) this).getBundledConfig());
        }
        return result;
    }


    @Override
    public String toString() {
        assert description != null;
        return description;
    }


    @Override
    public final int compareTo(@NotNull final ConfigurationLocation other) {
        int result = 0;
        // bundled configs go first, ordered by their position in the BundledConfig enum
        if (other instanceof BundledConfigurationLocation) {
            if (this instanceof BundledConfigurationLocation) {
                final int o1 = ((BundledConfigurationLocation) this).getBundledConfig().ordinal();
                final int o2 = ((BundledConfigurationLocation) other).getBundledConfig().ordinal();
                result = Integer.compare(o1, o2);
            } else {
                result = 1;
            }
        } else {
            if (this instanceof BundledConfigurationLocation) {
                result = -1;
            } else {
                result = compareStrings(getDescription(), other.getDescription());
                if (result == 0) {
                    result = compareStrings(getLocation(), other.getLocation());
                    if (result == 0) {
                        result = Objects.compare(getType(), other.getType());
                    }
                }
            }
        }
        return result;
    }

    private int compareStrings(@Nullable final String pStr1, @Nullable final String pStr2)
    {
        int result = 0;
        if (pStr1 != null) {
            if (pStr2 != null) {
                result = pStr1.compareTo(pStr2);
            } else {
                result = -1;
            }
        } else if (pStr2 != null) {
            result = 1;
        }
        return result;
    }


    public boolean isBlacklisted() {
        return blacklistedUntil > currentTimeMillis();
    }

    public long blacklistedForSeconds() {
        return Math.max((blacklistedUntil - currentTimeMillis()) / 1000, 0);
    }

    public void blacklist() {
        blacklistedUntil = currentTimeMillis() + BLACKLIST_TIME_MS;
    }

    public void removeFromBlacklist() {
        blacklistedUntil = 0L;
    }
}
