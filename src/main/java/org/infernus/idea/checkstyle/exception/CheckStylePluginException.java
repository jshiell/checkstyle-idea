package org.infernus.idea.checkstyle.exception;

/**
 * Exception thrown on a plug-in error.
 */
public class CheckStylePluginException extends RuntimeException {
    private static final long serialVersionUID = -2138216104879078592L;

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
