package org.infernus.idea.checkstyle.exception;

/**
 * A failure we understand well enough to describe in terms the user can act on.
 * <p>Its message is meant for display, so the service layer passes it through rather than replacing it with the
 * underlying Checkstyle error.</p>
 * <p><b>Important:</b> Be sure to throw it <em>only</em> from the 'csaccess' sourceset!</p>
 */
public class ActionableCheckstyleException extends CheckstyleServiceException {

    public ActionableCheckstyleException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
