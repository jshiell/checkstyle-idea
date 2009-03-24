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
    FILE

}
