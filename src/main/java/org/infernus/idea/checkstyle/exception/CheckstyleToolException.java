package org.infernus.idea.checkstyle.exception;

/**
 * Wrapper for an exception that occurred in the Checkstyle tool itself.
 * <p><b>Important:</b> Be sure to throw it <em>only</em> from the 'csaccess' sourceset!</p>
 */
public class CheckstyleToolException extends CheckstyleServiceException {

    /**
     * Constructor which copies the given cause's message into this instance.
     *
     * @param pCause the cause
     */
    public CheckstyleToolException(final Throwable pCause) {
        super(pCause.getMessage(), pCause);
    }
}
