package org.infernus.idea.checkstyle.exception;

/**
 * An exception that originates with the Checkstyle access layer (aka Checkstyle plugin service), but is <em>not</em>
 * a native CheckstyleException.
 * <p><b>Important:</b> Be sure to throw it <em>only</em> from the 'csaccess' sourceset!</p>
 */
public class CheckstyleServiceException extends CheckStylePluginException {

    public CheckstyleServiceException(final String message) {
        super(message);
    }

    public CheckstyleServiceException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
