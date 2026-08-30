package org.infernus.idea.checkstyle.exception;

public class ThirdPartyJarDownloadException extends RuntimeException {
    public ThirdPartyJarDownloadException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ThirdPartyJarDownloadException(final String message) {
        super(message);
    }
}
