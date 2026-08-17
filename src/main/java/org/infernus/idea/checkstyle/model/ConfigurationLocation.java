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

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import static com.intellij.openapi.util.Pair.pair;
import static java.lang.System.currentTimeMillis;
import static java.util.Objects.requireNonNullElse;
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

    private static final int ONE_SECOND = 1000;
    private static final long BLOCK_TIME_MS = ONE_SECOND * 60;

    /**
     * Checkstyle's {@code XmlLoader.LoadExternalDtdFeatureProvider.ENABLE_EXTERNAL_DTD_LOAD}. Checkstyle is not on
     * this source set's classpath, so the name is repeated here. Honouring it gives us the same behaviour as the
     * Checkstyle CLI and the Gradle plugin.
     */
    private static final String ENABLE_EXTERNAL_DTD_LOAD = "checkstyle.enableExternalDtdLoad";

    private static final String EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
    private static final String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    private final Map<String, String> properties = new HashMap<>();
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
        synchronized (this) {
            if (namedScope != null) {
                namedScope = NamedScopeHelper.getScopeByIdWithDefaultFallback(project, namedScope.getScopeId());
            }
        }
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

    /**
     * Get the URI that relative references within this configuration file should be resolved against.
     *
     * @return the base URI, or null if this location type has no meaningful base URI.
     */
    @Nullable
    public String baseUri() {
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

    /**
     * Scans the given stream for {@code ${}} properties.
     *
     * @param inputStream           the configuration file, or null if it could not be resolved.
     * @param checkstyleClassLoader the classloader for the configured Checkstyle.
     * @return the properties and their defaults, or {@link Optional#empty()} if the scan could not be completed.
     * An empty map means the scan completed and found no properties.
     */
    private Optional<Map<String, String>> extractProperties(@Nullable final InputStream inputStream,
                                                            @NotNull final ClassLoader checkstyleClassLoader) {
        if (inputStream != null) {
            try {
                if (externalDtdLoadIsEnabled()) {
                    return Optional.of(scanForPropertiesResolvingEntities(inputStream, checkstyleClassLoader));
                }
                return Optional.of(scanForProperties(inputStream, checkstyleClassLoader));

            } catch (Exception e) {
                LOG.warn("CheckStyle file could not be parsed for properties.", e);
            }
        }

        return Optional.empty();
    }

    static boolean externalDtdLoadIsEnabled() {
        return Boolean.parseBoolean(System.getProperty(ENABLE_EXTERNAL_DTD_LOAD));
    }

    private Map<String, String> scanForProperties(@NotNull final InputStream inputStream,
                                                  @NotNull final ClassLoader checkstyleClassLoader)
            throws XMLStreamException {
        final Map<String, String> propertiesAndDefaults = new HashMap<>();

        final XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setXMLResolver(new CheckStyleEntityResolver(this, checkstyleClassLoader));
        final XMLEventReader eventReader = factory.createXMLEventReader(inputStream);
        try {
            while (eventReader.hasNext()) {
                final XMLEvent event = eventReader.nextEvent();
                if (event.isStartElement()) {
                    final StartElement startElement = (StartElement) event;
                    if ("property".equals(startElement.getName().getLocalPart())) {
                        addPropertyIfPresent(propertiesAndDefaults,
                                valueOf(startElement, "value"),
                                valueOf(startElement, "default"));
                    }
                }
            }
        } finally {
            eventReader.close();
        }

        return propertiesAndDefaults;
    }

    /**
     * Scans for properties with entity resolution enabled, so that properties declared in an included file are
     * found. This uses SAX rather than StAX as the IDE supplies a StAX implementation that cannot expand external
     * general entities.
     */
    private Map<String, String> scanForPropertiesResolvingEntities(@NotNull final InputStream inputStream,
                                                                   @NotNull final ClassLoader checkstyleClassLoader)
            throws ParserConfigurationException, SAXException, IOException {
        final Map<String, String> propertiesAndDefaults = new HashMap<>();

        final SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        factory.setFeature(EXTERNAL_GENERAL_ENTITIES, true);
        factory.setFeature(LOAD_EXTERNAL_DTD, true);

        final XMLReader reader = factory.newSAXParser().getXMLReader();
        reader.setEntityResolver(new CheckStyleEntityResolver(this, checkstyleClassLoader));
        reader.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName,
                                     final String qName, final Attributes attributes) {
                if ("property".equals(localName)) {
                    addPropertyIfPresent(propertiesAndDefaults,
                            attributes.getValue("value"),
                            attributes.getValue("default"));
                }
            }
        });
        reader.parse(inputSourceFor(inputStream));

        return propertiesAndDefaults;
    }

    @NotNull
    private InputSource inputSourceFor(@NotNull final InputStream inputStream) {
        final InputSource inputSource = new InputSource(inputStream);

        final String baseUri = baseUri();
        if (baseUri != null) {
            // without this the parser has no base URI, and resolves relative entities against the working directory
            inputSource.setSystemId(baseUri);
        }

        return inputSource;
    }

    @Nullable
    private String valueOf(final StartElement startElement, final String attributeName) {
        final var attribute = startElement.getAttributeByName(new QName(attributeName));
        return attribute != null ? attribute.getValue() : null;
    }

    private static void addPropertyIfPresent(final Map<String, String> propertiesAndDefaults,
                                             @Nullable final String value,
                                             @Nullable final String defaultValue) {
        final var property = extractNameAndDefault(value, defaultValue);
        if (property != null) {
            propertiesAndDefaults.put(property.first, property.second);
        }
    }

    @Nullable
    private static Pair<String, String> extractNameAndDefault(@Nullable final String value,
                                                              @Nullable final String defaultValue) {
        if (value != null) {
            final int propertyStart = value.indexOf("${");
            final int propertyEnd = value.indexOf('}');
            if (propertyStart >= 0 && propertyEnd >= 0) {
                final String propertyName = value.substring(propertyStart + 2, propertyEnd);
                return pair(propertyName, requireNonNullElse(defaultValue, ""));
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
            final Optional<Map<String, String>> propertiesInFile = extractProperties(is, checkstyleClassLoader);

            propertiesInFile.ifPresent(this::reconcilePropertiesWith);

            try {
                is.reset();
            } catch (IOException e) {
                is = resolveFile(checkstyleClassLoader); // JAR IS doesn't support this, for instance
            }

            // if the scan failed we leave this false, so that a later fix to the file is picked up
            propertiesCheckedThisSession = propertiesInFile.isPresent();
        }

        return is;
    }

    private void reconcilePropertiesWith(final Map<String, String> propertiesInFile) {
        for (final String propertyName : propertiesInFile.keySet()) {
            if (!properties.containsKey(propertyName)) {
                properties.put(propertyName, propertiesInFile.getOrDefault(propertyName, ""));
            }
        }

        properties.keySet().removeIf(propertyName -> !propertiesInFile.containsKey(propertyName));
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
                || fileName.toLowerCase(Locale.ENGLISH).startsWith("http://")
                || fileName.toLowerCase(Locale.ENGLISH).startsWith("https://")) {
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
        result = java.util.Objects.hash(result, additionalHashCodeComponents());
        return result;
    }


    /**
     * Subclasses may override this to contribute additional components to {@link #hashCode()}.
     * The default implementation returns {@code null} (no additional contribution).
     *
     * @return an additional value to fold into the hash, or {@code null}
     */
    @Nullable
    protected Object additionalHashCodeComponents() {
        return null;
    }


    @Override
    public String toString() {
        assert description != null;
        return description;
    }

    @Override
    public final int compareTo(@NotNull final ConfigurationLocation other) {
        int result;
        if (other.isPrioritySortOrder()) {
            if (this.isPrioritySortOrder()) {
                result = compareForPrioritySortOrder(other);
            } else {
                result = 1;
            }
        } else {
            if (this.isPrioritySortOrder()) {
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

    /**
     * Returns {@code true} if this location has priority in the sorting.
     */
    protected boolean isPrioritySortOrder() {
        return false;
    }

    /**
     * Compares this location to another location by priority sort order.
     * Only called when both {@code this} and {@code other} satisfy {@link #isPrioritySortOrder()}.
     * The default implementation returns 0; subclasses that require priority must override it.
     */
    protected int compareForPrioritySortOrder(@NotNull final ConfigurationLocation other) {
        return 0;
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
        return Math.max((blockedUntil - currentTimeMillis()) / ONE_SECOND, 0);
    }

    public synchronized void block() {
        blockedUntil = currentTimeMillis() + BLOCK_TIME_MS;
    }

    public synchronized void unblock() {
        blockedUntil = 0L;
    }
}
