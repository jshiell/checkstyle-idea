package org.infernus.idea.checkstyle.exception;

import java.io.Serial;

public class CheckStylePluginParseException extends CheckStylePluginException {
    @Serial
    private static final long serialVersionUID = -2138216104879079892L;

    public CheckStylePluginParseException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
