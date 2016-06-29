package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;

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
     * @return the location.
     */
    public ConfigurationLocation create(final Project project,
                                        final ConfigurationType type,
                                        final String location,
                                        final String description) {
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
                configurationLocation = new HTTPURLConfigurationLocation();
                break;

            case INSECURE_HTTP_URL:
                configurationLocation = new InsecureHTTPURLConfigurationLocation();
                break;

            case CLASSPATH:
                configurationLocation = new ClassPathConfigurationLocation();
                break;

            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
        configurationLocation.setLocation(location);
        configurationLocation.setDescription(description);

        synchronized (instanceCache) {
            if (instanceCache.containsKey(configurationLocation)) {
                return instanceCache.get(configurationLocation);

            } else {
                instanceCache.put(configurationLocation, configurationLocation);
            }
        }

        return configurationLocation;
    }

    /**
     * Create a new location from a string representation.
     *
     * @param project              the project this location is associated with.
     * @param stringRepresentation the toString of another ConfigurationLocation.
     * @return the location.
     */
    public ConfigurationLocation create(final Project project, final String stringRepresentation) {
        if (project == null) {
            throw new IllegalArgumentException("A project is required");
        }

        if (isBlank(stringRepresentation)) {
            throw new IllegalArgumentException("A non-blank representation is required");
        }

        final int typeSplitIndex = stringRepresentation.indexOf(':');
        if (typeSplitIndex <= 0 || typeSplitIndex >= stringRepresentation.length() - 1) {
            throw new IllegalArgumentException("Invalid string representation: " + stringRepresentation);
        }

        final String typeString = stringRepresentation.substring(0, typeSplitIndex);


        final int descriptionSplitIndex = stringRepresentation.lastIndexOf(':');
        if (descriptionSplitIndex <= 0) {
            throw new IllegalArgumentException("Invalid string representation: " + stringRepresentation);
        }

        final String location = stringRepresentation.substring(typeSplitIndex + 1, descriptionSplitIndex);
        String description = "";
        if (descriptionSplitIndex < (stringRepresentation.length() - 1)) {
            description = stringRepresentation.substring(descriptionSplitIndex + 1);
        }

        final ConfigurationType type = ConfigurationType.parse(typeString);

        return create(project, type, location, description);
    }
}
