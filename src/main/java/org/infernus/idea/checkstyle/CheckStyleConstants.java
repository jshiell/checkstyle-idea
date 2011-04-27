package org.infernus.idea.checkstyle;

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
     * The extension of temporary files.
     */
    public static final String TEMPFILE_EXTENSION = ".java";

    /**
     * Plug-in identifier.
     */
    public static final String ID_PLUGIN = "CheckStyle-IDEA";

    /**
     * Inspection identifier.
     */
    public static final String ID_INSPECTION = "CheckStyleIDEAInspection";

    /**
     * Plug-in module identifier.
     */
    public static final String ID_MODULE_PLUGIN = "CheckStyle-IDEA-Module";

    /**
     * Tool Window identifier.
     */
    public static final String ID_TOOLWINDOW = "CheckStyle";

    /**
     * Constant used to represent project directory.
     */
    public static final String PROJECT_DIR = "$PROJECT_DIR$";

    /**
     * This is a constants class and cannot be instantiated.
     */
    private CheckStyleConstants() {
        // constants class
    }

}
