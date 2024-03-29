package org.infernus.idea.checkstyle.exception;


import java.io.Serial;

/**
 * Common exception thrown anywhere in this plugin.
 */
public class CheckStylePluginException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 2L;

    public CheckStylePluginException(final String message) {
        super(message);
    }

    public CheckStylePluginException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
