package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;

/**
 *
 */
public class ConfigurationLocationFactory {

    /**
     * Create a new location.
     *
     * @param project     the project this location is associated with.
     * @param type        the type.
     * @param location    the location.
     * @param description the optional description.
     * @return the location.
     */
    public static ConfigurationLocation create(final Project project,
                                               final ConfigurationType type,
                                               final String location,
                                               final String description) {
        if (type == null) {
            throw new IllegalArgumentException("Type is required");
        }

        switch (type) {
            case FILE:
                return new FileConfigurationLocation(project, location, description);

            case HTTP_URL:
                return new HTTPURLConfigurationLocation(location, description);

            case CLASSPATH:
                return new ClassPathConfigurationLocation(location, description);

            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    /**
     * Create a new location from a string representation.
     *
     * @param project              the project this location is associated with.
     * @param stringRepresentation the toString of another ConfigurationLocation.
     */
    public static ConfigurationLocation create(final Project project, final String stringRepresentation) {
        if (project == null) {
            throw new IllegalArgumentException("A project is required");
        }

        if (stringRepresentation == null || stringRepresentation.trim().length() == 0) {
            throw new IllegalArgumentException("A non-blank representation is required");
        }

        final int typeSplitIndex = stringRepresentation.indexOf(":");
        if (typeSplitIndex <= 0 || typeSplitIndex >= stringRepresentation.length() - 1) {
            throw new IllegalArgumentException("Invalid string representation: " + stringRepresentation);
        }

        final String typeString = stringRepresentation.substring(0, typeSplitIndex);


        final int descriptionSplitIndex = stringRepresentation.lastIndexOf(":");
        if (descriptionSplitIndex <= 0 || descriptionSplitIndex >= stringRepresentation.length() - 1) {
            throw new IllegalArgumentException("Invalid string representation: " + stringRepresentation);
        }

        final String location = stringRepresentation.substring(typeSplitIndex + 1, descriptionSplitIndex);
        final String description = stringRepresentation.substring(descriptionSplitIndex + 1);


        final ConfigurationType type = ConfigurationType.valueOf(typeString);

        switch (type) {
            case FILE:
                return new FileConfigurationLocation(project, location, description);

            case HTTP_URL:
                return new HTTPURLConfigurationLocation(location, description);

            case CLASSPATH:
                return new ClassPathConfigurationLocation(location, description);

            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }
}
