package org.infernus.idea.checkstyle.model;

/**
 * Types of configuration supported.
 */
public enum ConfigurationType {

    /**
     * Located on the IDE classpath.
     */
    CLASSPATH,

    /**
     * Located on a HTTP URL.
     */
    HTTP_URL,

    /**
     * Located in a local file.
     */
    LOCAL_FILE,

    /**
     * Located in a local file where the path is project relative.
     */
    PROJECT_RELATIVE;

    /**
     * Parse a case-insensitive type string.
     *
     * @param typeAsString the type, as a string.
     * @return the type.
     */
    public static ConfigurationType parse(final String typeAsString) {
        if (typeAsString == null) {
            return null;
        }

        final String processedType = typeAsString.toUpperCase().replace(' ', '_');
        if (processedType.equals("FILE")) {
            return LOCAL_FILE;
        }

        return valueOf(processedType);
    }

}
