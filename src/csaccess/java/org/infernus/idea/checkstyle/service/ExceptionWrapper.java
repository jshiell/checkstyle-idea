package org.infernus.idea.checkstyle.service;

import java.util.HashSet;
import java.util.Set;

import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.exception.CheckStylePluginParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;


public class ExceptionWrapper {

    private static final Set<String> NON_JVM_PARSE_EXCEPTIONS = new HashSet<>(asList(
            "antlr.RecognitionException",
            "antlr.TokenStreamException",
            "org.antlr.v4.runtime.RecognitionException"));

    private final Set<Class<? extends Throwable>> parseExceptions = new HashSet<>(asList(
            NullPointerException.class,
            ArrayIndexOutOfBoundsException.class,
            StringIndexOutOfBoundsException.class,
            IllegalStateException.class,
            ClassCastException.class));

    @SuppressWarnings("unchecked")
    public ExceptionWrapper() {
        NON_JVM_PARSE_EXCEPTIONS.forEach(exceptionName -> {
            try {
                Class<?> aClass = Class.forName(exceptionName);
                if (Throwable.class.isAssignableFrom(aClass)) {
                    parseExceptions.add((Class<? extends Throwable>) aClass);
                }
            } catch (ClassNotFoundException ignored) {
                // The versions of Antlr used by Checkstyle differ, so these aren't always available
            }
        });
    }

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
            final Class<? extends Throwable> causeClass = throwable.getCause().getClass();
            for (Class<? extends Throwable> parseExceptionType : parseExceptions) {
                if (parseExceptionType.isAssignableFrom(causeClass)) {
                    return true;
                }
            }

            return isAntlrException(causeClass);
        }
        return false;
    }

    private boolean isAntlrException(final Class<? extends Throwable> causeClass) {
        final String causeClassPackage = causeClass.getPackage().getName();
        return causeClassPackage.startsWith("antlr.") || causeClassPackage.startsWith("org.antlr.");
    }
}
