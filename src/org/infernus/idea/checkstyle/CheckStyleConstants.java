package org.infernus.idea.checkstyle;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;

/**
 * Cross-application constants for the CheckStyle plug-in.
 *
 * @author James Shiell
 * @version 1.1
 */
public final class CheckStyleConstants {

    /**
     * The name of the application resource bundle.
     */
    public static final String RESOURCE_BUNDLE
            = "org.infernus.idea.checkstyle.resource";

    /**
     * IDEA filetype for Java files.
     */
    public static final FileType FILETYPE_JAVA
            = FileTypeManager.getInstance().getFileTypeByExtension("java");

    /**
     * The prefix of the temporary files.
     */
    public static final String TEMPFILE_NAME = "checkstyle-idea";

    /**
     * The extension of temporary files.
     */
    public static final String TEMPFILE_EXTENSION = ".java";

    /**
     * The CP location of the default CheckStyle configuration.
     */
    public static final String DEFAULT_CONFIG = "/sun_checks.xml";

    /**
     * XML element name in the IDEA configuration file for the path
     * to the CS configuration file.
     */
    public static final String CONFIG_FILEPATH_ELEMENT = "config-file";

    /**
     * Plug-in identifier.
     */
    public static final String ID_PLUGIN = "CheckStyle-IDEA";

    /**
     * The action group for the plug-in tool window.
     */
    public static final String ACTION_GROUP = "CheckStylePluginActions";

    /**
     * This is a constants class and cannot be instantiated.
     */
    private CheckStyleConstants() {
        // constants class
    }

}
