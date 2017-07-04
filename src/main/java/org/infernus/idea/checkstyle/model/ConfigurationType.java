package org.infernus.idea.checkstyle.model;


/**
 * Types of configuration supported.
 */
public enum ConfigurationType
{
    /**
     * one of the configurations bundled with the Checkstyle tool, chosen from the
     * {@link org.infernus.idea.checkstyle.csapi.BundledConfig} enum
     */
    BUNDLED,

    /**
     * Located on a HTTP URL.
     */
    HTTP_URL,

    /**
     * Located on a HTTP URL where the SSL context is naughtily ignored.
     */
    INSECURE_HTTP_URL,

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
        if ("FILE".equals(processedType)) {
            return LOCAL_FILE;
        }

        return valueOf(processedType);
    }

}
