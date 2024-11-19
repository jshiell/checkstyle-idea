package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packageDependencies.DependencyValidationManager;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.NamedScopeManager;
import org.infernus.idea.checkstyle.checker.CheckerFactoryCache;
import org.infernus.idea.checkstyle.util.CheckStyleEntityResolver;
import org.infernus.idea.checkstyle.util.Objects;
import org.infernus.idea.checkstyle.util.ProjectPaths;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.intellij.openapi.util.Pair.pair;
import static java.lang.System.currentTimeMillis;
import static org.infernus.idea.checkstyle.util.Strings.isBlank;

/**
 * Bean encapsulating a configuration source.
 * <p>Note on identity: Configuration locations are considered equal if their descriptor matches. The descriptor
 * consists of type, location, and description text. Properties are not considered.</p>
 * <p>Note on order: Configuration locations are ordered by description text, followed by location and type, except that
 * the bundled configurations (Sun and Google checks) always go first.</p>
 */
public abstract class ConfigurationLocation implements Cloneable, Comparable<ConfigurationLocation> {
    private static final Logger LOG = Logger.getInstance(ConfigurationLocation.class);

    private static final long BLOCK_TIME_MS = 1000 * 60;

    private final Map<String, String> properties = new ConcurrentHashMap<>();
    private final String id;
    private final ConfigurationType type;
    private final Project project;
    private String location;
    private String description;
    private NamedScope namedScope;

    private boolean propertiesCheckedThisSession;
    private long blockedUntil;

    public ConfigurationLocation(@NotNull final String id,
                                 @NotNull final ConfigurationType type,
                                 @NotNull final Project project) {
        this.id = id;
        this.type = type;
        this.project = project;
        this.namedScope = NamedScopeHelper.getDefaultScope(project);
        this.initializeFutureScopeChangeHandling();
    }

    /**
     * Refreshes the named scope if the scopes have been changed.
     */
    private void initializeFutureScopeChangeHandling() {
        Disposable parent = project.getService(CheckerFactoryCache.class);
        NamedScopeManager.getInstance(project).addScopeListener(this::scopeChanged, parent);
        DependencyValidationManager.getInstance(project).addScopeListener(this::scopeChanged, parent);
    }

    private void scopeChanged() {
        this.getNamedScope().ifPresent(scope ->
                this.setNamedScope(NamedScopeHelper.getScopeByIdWithDefaultFallback(project, scope.getScopeId())));
    }

    public boolean canBeResolvedInDefaultProject() {
        return true;
    }

    protected final Project getProject() {
        return project;
    }

    /**
     * Get the base directory for this checkstyle file. If null then the project directory is assumed.
     *
     * @return the base directory for the file, or null if not applicable to the location type.
     */
    public File getBaseDir() {
        return null;
    }

    @NotNull
    public String getId() {
        return id;
    }

    public ConfigurationType getType() {
        return type;
    }

    public synchronized String getLocation() {
        return location;
    }

    public final synchronized String getRawLocation() {
        return location;
    }

    public synchronized Optional<NamedScope> getNamedScope() {
        return Optional.ofNullable(this.namedScope);
    }

    public synchronized void setLocation(final String location) {
        if (isBlank(location)) {
            throw new IllegalArgumentException("A non-blank location is required");
        }

        this.location = location;
        if (description == null) {
            description = location;
        }

        this.propertiesCheckedThisSession = false;
    }

    public synchronized String getDescription() {
        return description;
    }

    public synchronized void setDescription(@Nullable final String description) {
        if (description == null) {
            this.description = location;
        } else {
            this.description = description;
        }
    }

    public synchronized void setNamedScope(final NamedScope namedScope) {
        this.namedScope = namedScope;
    }

    public synchronized Map<String, String> getProperties() {
        return new HashMap<>(properties);
    }

    public synchronized void setProperties(final Map<String, String> newProperties) {
        properties.clear();

        if (newProperties == null) {
            return;
        }

        properties.putAll(newProperties);

        this.propertiesCheckedThisSession = false;
    }

    public synchronized boolean isRemovable() {
        return true;
    }

    public synchronized void reset() {
        propertiesCheckedThisSession = false;
        unblock();
    }

    private Map<String, String> extractProperties(@Nullable final InputStream inputStream,
                                           @NotNull final ClassLoader checkstyleClassLoader) {
        if (inputStream != null) {
            try {
                final Map<String, String> propertiesAndDefaults = new HashMap<>();

                final XMLInputFactory factory = XMLInputFactory.newInstance();
                factory.setXMLResolver(new CheckStyleEntityResolver(this, checkstyleClassLoader));
                final XMLEventReader eventReader = factory.createXMLEventReader(inputStream);

                while (eventReader.hasNext()) {
                    final XMLEvent event = eventReader.nextEvent();

                    if (event.isStartElement()) {
                        final var property = extractNameAndDefaultIfPropertyElement((StartElement) event);
                        if (property != null) {
                            propertiesAndDefaults.put(property.first, property.second);
                        }
                    }
                }

                return propertiesAndDefaults;

            } catch (Exception e) {
                LOG.warn("CheckStyle file could not be parsed for properties.", e);
            }
        }

        return Collections.emptyMap();
    }

    private static Pair<String, String> extractNameAndDefaultIfPropertyElement(final StartElement startElement) {
        if ("property".equals(startElement.getName().getLocalPart())) {
            final var valueAttribute = startElement.getAttributeByName(new QName("value"));
            if (valueAttribute != null) {
                final String value = valueAttribute.getValue();
                final int propertyStart = value.indexOf("${");
                final int propertyEnd = value.indexOf('}');
                if (propertyStart >= 0 && propertyEnd >= 0) {
                    final String propertyName = value.substring(propertyStart + 2, propertyEnd);

                    final var defaultAttribute = startElement.getAttributeByName(new QName("default"));
                    if (defaultAttribute != null) {
                        return pair(propertyName, defaultAttribute.getValue());
                    }
                    return pair(propertyName, "");
                }
            }
        }
        return null;
    }

    @SuppressWarnings("EmptyTryBlock")
    public synchronized void ensurePropertiesAreUpToDate(@NotNull final ClassLoader checkstyleClassLoader) throws IOException {
        if (!propertiesCheckedThisSession) {
            try (InputStream ignored = resolve(checkstyleClassLoader)) {
                // ignored
            }
        }
    }

    public synchronized InputStream resolve(@NotNull final ClassLoader checkstyleClassLoader) throws IOException {
        InputStream is = resolveFile(checkstyleClassLoader);

        if (!propertiesCheckedThisSession) {
            final Map<String, String> propertiesInFile = extractProperties(is, checkstyleClassLoader);

            for (final String propertyName : propertiesInFile.keySet()) {
                if (!properties.containsKey(propertyName)) {
                    properties.put(propertyName, propertiesInFile.getOrDefault(propertyName, ""));
                }
            }

            properties.keySet().removeIf(propertyName -> !propertiesInFile.containsKey(propertyName));

            try {
                is.reset();
            } catch (IOException e) {
                is = resolveFile(checkstyleClassLoader); // JAR IS doesn't support this, for instance
            }

            propertiesCheckedThisSession = true;
        }

        return is;
    }

    @Nullable
    public synchronized String resolveAssociatedFile(@Nullable final String filename,
                                                     @Nullable final Module module,
                                                     @NotNull final ClassLoader checkstyleClassLoader) throws IOException {
        if (filename == null) {
            return null;
        } else if (new File(filename).exists()) {
            return filename;
        }

        return findFile(filename, module, checkstyleClassLoader);
    }

    private String findFile(final String fileName,
                            final Module module,
                            final ClassLoader checkstyleClassLoader) {
        if (fileName == null
                || fileName.trim().isEmpty()
                || fileName.toLowerCase().startsWith("http://")
                || fileName.toLowerCase().startsWith("https://")) {
            return fileName;
        }

        File targetFile = checkCommonPathsForTarget(fileName, module);
        if (targetFile != null) {
            return targetFile.getAbsolutePath();
        }

        if (existsOnClasspath(fileName, checkstyleClassLoader)) {
            return fileName;
        }

        return null;
    }

    private boolean existsOnClasspath(final String fileName,
                                      final ClassLoader checkstyleClassLoader) {
        if (fileName.startsWith("/")) {
            return checkstyleClassLoader.getResource(fileName.substring(1)) != null;
        }
        return checkstyleClassLoader.getResource(fileName) != null;
    }

    private File checkCommonPathsForTarget(final String fileName,
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
            targetFile = checkProjectBaseDir(fileName);
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

    private File checkProjectBaseDir(final String fileName) {
        VirtualFile baseDir = projectPaths().projectPath(project);
        if (baseDir != null) {
            final File projectRelativePath = new File(baseDir.getPath(), fileName);
            if (projectRelativePath.exists()) {
                return projectRelativePath;
            }
        }
        return null;
    }

    private File checkModuleFile(final Module module,
                                 final String fileName) {
        VirtualFile moduleDir = projectPaths().modulePath(module);
        if (moduleDir != null) {
            final File moduleRelativePath = new File(moduleDir.getPath(), fileName);
            if (moduleRelativePath.exists()) {
                return moduleRelativePath;
            }
        }
        return null;
    }

    private File checkModuleContentRoots(final Module module, final String fileName) {
        ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
        for (final ContentEntry contentEntry : rootManager.getContentEntries()) {
            if (contentEntry.getFile() == null) {
                continue;
            }

            final File contentEntryPath = new File(contentEntry.getFile().getPath(), fileName);
            if (contentEntryPath.exists()) {
                return contentEntryPath;
            }
        }
        return null;
    }

    public final synchronized boolean hasChangedFrom(final ConfigurationLocation configurationLocation) {
        return !equals(configurationLocation)
                || propertiesHaveChanged(configurationLocation);
    }

    private boolean propertiesHaveChanged(final ConfigurationLocation configurationLocation) {
        if (project.isDefault() && !configurationLocation.canBeResolvedInDefaultProject()) {
            return false;
        }
        return !getProperties().equals(configurationLocation.getProperties());
    }

    /**
     * Resolve this location to a file.
     *
     * @param checkstyleClassLoader the classloader for the configured Checkstyle.
     * @return the file to load.
     * @throws IOException if the file cannot be loaded.
     */
    @NotNull
    protected abstract InputStream resolveFile(@NotNull ClassLoader checkstyleClassLoader) throws IOException;

    @Override
    public abstract Object clone();

    ConfigurationLocation cloneCommonPropertiesTo(final ConfigurationLocation cloned) {
        cloned.setDescription(getDescription());
        cloned.setLocation(getLocation());
        cloned.setProperties(new HashMap<>(getProperties()));
        cloned.setNamedScope(getNamedScope().orElse(NamedScopeHelper.getDefaultScope(project)));
        return cloned;
    }


    @Override
    public final boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ConfigurationLocation that)) {
            return false;
        }
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
        int result;
        // bundled configs go first, ordered by their position in the BundledConfig enum
        if (other instanceof BundledConfigurationLocation) {
            if (this instanceof BundledConfigurationLocation) {
                final int o1 = ((BundledConfigurationLocation) this).getBundledConfig().getSortOrder();
                final int o2 = ((BundledConfigurationLocation) other).getBundledConfig().getSortOrder();
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

    private int compareStrings(@Nullable final String pStr1, @Nullable final String pStr2) {
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

    @NotNull
    protected ProjectPaths projectPaths() {
        return getProject().getService(ProjectPaths.class);
    }

    public synchronized boolean isBlocked() {
        return blockedUntil > currentTimeMillis();
    }

    public synchronized long blockedForSeconds() {
        return Math.max((blockedUntil - currentTimeMillis()) / 1000, 0);
    }

    public synchronized void block() {
        blockedUntil = currentTimeMillis() + BLOCK_TIME_MS;
    }

    public synchronized void unblock() {
        blockedUntil = 0L;
    }
}
