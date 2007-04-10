package org.infernus.idea.checkstyle;

/**
 * Exception thrown on a plug-in error.
 *
 * @author James Shiell
 * @version 1.0
 */
public class CheckStylePluginException extends RuntimeException {

    /**
     * Create a new exception with no cause.
     *
     * @param message the error message.
     */
    public CheckStylePluginException(final String message) {
        super(message);
    }

    /**
     * Create a new exception with the given cause.
     *
     * @param message the error message.
     * @param cause the cause.
     */
    public CheckStylePluginException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
