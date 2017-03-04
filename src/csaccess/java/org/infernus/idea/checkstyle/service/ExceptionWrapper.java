package org.infernus.idea.checkstyle.service;

import java.util.HashSet;
import java.util.Set;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.exception.CheckStylePluginParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;


public class ExceptionWrapper {

    private static final Set<Class<? extends Throwable>> PARSE_EXCEPTIONS = new HashSet<>(asList(
            RecognitionException.class,
            TokenStreamException.class,
            NullPointerException.class,
            ArrayIndexOutOfBoundsException.class,
            StringIndexOutOfBoundsException.class,
            IllegalStateException.class));


    public CheckStylePluginException wrap(@Nullable final String message, @NotNull final Throwable error) {
        final Throwable root = rootOrCheckStyleException(error);
        final String exMessage = ofNullable(message).orElseGet(root::getMessage);

        if (isParseException(root)) {
            return new CheckStylePluginParseException(exMessage, root);
        }
        return new CheckStylePluginException(exMessage, root);
    }


    private Throwable rootOrCheckStyleException(final Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && notBaseCheckstyleException(root)) {
            root = root.getCause();
        }
        return root;
    }


    private boolean notBaseCheckstyleException(final Throwable root) {
        return !(root instanceof CheckstyleException && !(root.getCause() instanceof CheckstyleException));
    }


    private boolean isParseException(final Throwable throwable) {
        if (throwable instanceof CheckstyleException && throwable.getCause() != null) {
            for (Class<? extends Throwable> parseExceptionType : PARSE_EXCEPTIONS) {
                if (parseExceptionType.isAssignableFrom(throwable.getCause().getClass())) {
                    return true;
                }
            }
        }
        return false;
    }
}
