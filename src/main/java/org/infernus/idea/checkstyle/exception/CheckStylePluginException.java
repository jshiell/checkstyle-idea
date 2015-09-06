package org.infernus.idea.checkstyle.exception;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Optional.ofNullable;

public class CheckStylePluginException extends RuntimeException {
    private static final long serialVersionUID = -2138216104879078592L;

    public CheckStylePluginException(final String message) {
        super(message);
    }

    public CheckStylePluginException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public static CheckStylePluginException wrap(@NotNull final Throwable error) {
        return wrap(null, error);
    }

    public static CheckStylePluginException wrap(@Nullable final String message,
                                                 @NotNull final Throwable error) {
        final Throwable root = rootOrCheckStyleException(error);
        final String exMessage = ofNullable(message).orElseGet(root::getMessage);

        if (isParseException(root)) {
            return new CheckStylePluginParseException(exMessage, root);
        }
        return new CheckStylePluginException(exMessage, root);
    }

    private static Throwable rootOrCheckStyleException(final Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && !(root instanceof CheckstyleException)) {
            root = root.getCause();
        }
        return root;
    }

    private static boolean isParseException(final Throwable throwable) {
        if (throwable instanceof CheckstyleException) {
            final CheckstyleException checkstyleException = (CheckstyleException) throwable;
            return checkstyleException.getCause() instanceof RecognitionException
                    || checkstyleException.getCause() instanceof TokenStreamException;
        }
        return false;
    }
}
