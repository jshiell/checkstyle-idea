package org.infernus.idea.checkstyle.exception;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;

public class CheckStylePluginException extends RuntimeException {
    private static final long serialVersionUID = -2138216104879078592L;

    private static final Set<Class<? extends Throwable>> PARSE_EXCEPTIONS = new HashSet<>(asList(
            RecognitionException.class,
            TokenStreamException.class,
            NullPointerException.class,
            ArrayIndexOutOfBoundsException.class,
            IllegalStateException.class));

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
        while (root.getCause() != null && notBaseCheckstyleException(root)) {
            root = root.getCause();
        }
        return root;
    }

    private static boolean notBaseCheckstyleException(final Throwable root) {
        return !(root instanceof CheckstyleException
                && !(root.getCause() instanceof CheckstyleException));
    }

    private static boolean isParseException(final Throwable throwable) {
        if (throwable instanceof CheckstyleException) {
            for (Class<? extends Throwable> parseExceptionType : PARSE_EXCEPTIONS) {
                if (parseExceptionType.isAssignableFrom(throwable.getCause().getClass())) {
                    return true;
                }
            }
        }
        return false;
    }
}
