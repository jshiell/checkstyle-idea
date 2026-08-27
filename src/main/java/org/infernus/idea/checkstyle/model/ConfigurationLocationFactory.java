package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.WeakHashMap;


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
                                                 final String id,
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
                configurationLocation = new FileConfigurationLocation(project, id);
                break;

            case PROJECT_RELATIVE:
                configurationLocation = new RelativeFileConfigurationLocation(project, id);
                break;

            case HTTP_URL:
                configurationLocation = new HTTPURLConfigurationLocation(project, id);
                break;

            case INSECURE_HTTP_URL:
                configurationLocation = new InsecureHTTPURLConfigurationLocation(project, id);
                break;

            case PLUGIN_CLASSPATH:
                configurationLocation = new ClasspathConfigurationLocation(project, id);
                break;

            case BUNDLED:
                configurationLocation = resolveBundledConfigurationLocation(project, id, location, description);
                break;

            case LEGACY_CLASSPATH:
                return create(BundledConfig.SUN_CHECKS, project); // backwards compatibility with old config files

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


    public @NotNull BundledConfigurationLocation create(@NotNull final BundledConfig bundledConfig,
                                                        @NotNull final Project project) {
        return new BundledConfigurationLocation(bundledConfig, project);
    }

    /**
     * Resolve a {@link BundledConfigurationLocation} for a {@code BUNDLED}-type descriptor.
     * <p>Tries, in order: (1) match {@code location} against a {@link BundledConfig} id, keeping the
     * caller-supplied {@code id} - this is the path for both freshly-seeded and user-created copies, which
     * need a stable id of their own; (2) match {@code description} against a {@link BundledConfig}'s canonical
     * description, using the {@link BundledConfig}'s own id - this is the legacy-migration path, where the
     * caller only ever supplies a fresh random id that must be discarded in favour of the stable canonical one;
     * (3) fall back to the {@link BundledConfig#fromDescription(String)} heuristic, also using the
     * {@link BundledConfig}'s own id.</p>
     */
    @NotNull
    private BundledConfigurationLocation resolveBundledConfigurationLocation(@NotNull final Project project,
                                                                             @NotNull final String id,
                                                                             final String location,
                                                                             final String description) {
        for (final BundledConfig bundledConfig : BundledConfig.getAllBundledConfigs()) {
            if (bundledConfig.getId().equals(location)) {
                return new BundledConfigurationLocation(id, bundledConfig, project);
            }
        }
        for (final BundledConfig bundledConfig : BundledConfig.getAllBundledConfigs()) {
            if (bundledConfig.getDescription().equals(description)) {
                return new BundledConfigurationLocation(bundledConfig, project);
            }
        }
        return new BundledConfigurationLocation(BundledConfig.fromDescription(description), project);
    }

}
