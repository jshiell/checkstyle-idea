package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.WeakHashMap;

import static org.infernus.idea.checkstyle.util.Strings.isBlank;


/**
 * Factory for configuration location objects.
 */
public class ConfigurationLocationFactory {

    /**
     * We maintain a map of all current locations, to avoid recreating identical objects.
     * This allows us to ensure that updates to one location (e.g. a URL change) are visible
     * to other modules with a reference to the given location.
     */
    private final Map<ConfigurationLocation, ConfigurationLocation> instanceCache
            = new WeakHashMap<>();

    /**
     * Create a new location.
     *
     * @param project     the project this location is associated with.
     * @param type        the type.
     * @param location    the location.
     * @param description the optional description.
     * @param namedScope  the {@link NamedScope} for this ConfigurationLocation
     * @return the location.
     */
    public @NotNull ConfigurationLocation create(final Project project,
                                                 final ConfigurationType type,
                                                 final String location,
                                                 final String description,
                                                 final NamedScope namedScope) {
        if (type == null) {
            throw new IllegalArgumentException("Type is required");
        }

        ConfigurationLocation configurationLocation;

        switch (type) {
            case LOCAL_FILE:
                configurationLocation = new FileConfigurationLocation(project);
                break;

            case PROJECT_RELATIVE:
                configurationLocation = new RelativeFileConfigurationLocation(project);
                break;

            case HTTP_URL:
                configurationLocation = new HTTPURLConfigurationLocation(project);
                break;

            case INSECURE_HTTP_URL:
                configurationLocation = new InsecureHTTPURLConfigurationLocation(project);
                break;

            case PLUGIN_CLASSPATH:
                configurationLocation = new ClasspathConfigurationLocation(project);
                break;

            case BUNDLED:
                configurationLocation = new BundledConfigurationLocation(BundledConfig.fromDescription(description), project);
                break;

            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
        configurationLocation.setLocation(location);
        configurationLocation.setDescription(description);
        configurationLocation.setNamedScope(namedScope);

        synchronized (instanceCache) {
            ConfigurationLocation cachedLocation = instanceCache.get(configurationLocation);
            if (cachedLocation != null) {
                return cachedLocation;
            } else {
                instanceCache.put(configurationLocation, configurationLocation);
            }
        }

        return configurationLocation;
    }


    /**
     * Create a new location from a serialized string representation. For example when reading the plugin configuration
     * XML.
     *
     * @param project              the project this location is associated with.
     * @param stringRepresentation the toString of another ConfigurationLocation.
     * @return the location
     */
    public @NotNull ConfigurationLocation create(final Project project, final String stringRepresentation) {
        if (project == null) {
            throw new IllegalArgumentException("A project is required");
        }

        if (isBlank(stringRepresentation)) {
            throw new IllegalArgumentException("A non-blank representation is required");
        }

        final int typeSplitIndex = stringRepresentation.indexOf(':');
        if (indexIsOutOfBounds(typeSplitIndex, stringRepresentation)) {
            throw new IllegalArgumentException("Invalid string representation: " + stringRepresentation);
        }
        final String typeString = stringRepresentation.substring(0, typeSplitIndex);

        final int descriptionAndScopeSplitIndex = stringRepresentation.lastIndexOf(':');
        if (typeSplitIndex == descriptionAndScopeSplitIndex
                || indexIsOutOfBounds(descriptionAndScopeSplitIndex, stringRepresentation)) {
            throw new IllegalArgumentException("Invalid string representation: " + stringRepresentation);
        }
        final String location = stringRepresentation.substring(typeSplitIndex + 1, descriptionAndScopeSplitIndex);


        String description = "";
        String scopeString = "";
        if (descriptionAndScopeSplitIndex < (stringRepresentation.length() - 1)) {
            String descriptionAndScope = stringRepresentation.substring(descriptionAndScopeSplitIndex + 1);
            final String[] split = descriptionAndScope.split(";");
            description = split[0];
            scopeString = split.length < 2 ? NamedScopeHelper.DEFAULT_SCOPE_ID : split[1];
        }

        if ("CLASSPATH".equals(typeString)) {
            return create(BundledConfig.SUN_CHECKS, project);   // backwards compatibility with old config files
        }
        final ConfigurationType type = ConfigurationType.parse(typeString);
        return create(
                project,
                type,
                location,
                description,
                NamedScopeHelper.getScopeByIdWithDefaultFallback(project, scopeString));
    }

    public @NotNull BundledConfigurationLocation create(@NotNull final BundledConfig bundledConfig,
                                                        @NotNull final Project project) {
        return new BundledConfigurationLocation(bundledConfig, project);
    }

    private boolean indexIsOutOfBounds(final int index, final String stringRepresentation) {
        return index <= 0 || index >= stringRepresentation.length() - 1;
    }
}
