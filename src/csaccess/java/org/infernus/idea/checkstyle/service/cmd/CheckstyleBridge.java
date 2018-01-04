package org.infernus.idea.checkstyle.service.cmd;

import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public final class CheckstyleBridge {

    private CheckstyleBridge() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> messagesFrom(@NotNull final Configuration source) {
        // Checkstyle 8.7 changes the return type of Configuration.getMessages()
        Method getMessagesMethod = null;
        try {
            getMessagesMethod = Configuration.class.getDeclaredMethod("getMessages");
        } catch (NoSuchMethodException ignored) {
        }

        if (getMessagesMethod == null) {
            throw new RuntimeException("Unable to find usable getMessages method on configuration");
        }

        try {
            return (Map<String, String>) getMessagesMethod.invoke(source);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Unable to invoke getMessages method on configuration", e);
        }
    }
}
