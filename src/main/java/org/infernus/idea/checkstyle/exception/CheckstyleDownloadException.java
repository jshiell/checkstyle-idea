package org.infernus.idea.checkstyle.exception;

public class CheckstyleDownloadException extends RuntimeException {
    public CheckstyleDownloadException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public CheckstyleDownloadException(final String message) {
        super(message);
    }
}
