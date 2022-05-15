package org.infernus.idea.checkstyle.model;


public enum ConfigurationType {
    /**
     * one of the configurations bundled with the Checkstyle tool, chosen from the
     * {@link org.infernus.idea.checkstyle.csapi.BundledConfig} enum
     */
    BUNDLED,

    HTTP_URL,

    /**
     * Located on a HTTP URL where the SSL context is naughtily ignored.
     */
    INSECURE_HTTP_URL,

    LOCAL_FILE,

    /**
     * Located in a local file where the path is project relative.
     */
    PROJECT_RELATIVE,

    PLUGIN_CLASSPATH,

    LEGACY_CLASSPATH; // legacy


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
        } else if ("CLASSPATH".equals(processedType)) {
            return LEGACY_CLASSPATH;
        }

        return valueOf(processedType);
    }

}
