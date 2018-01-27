package org.infernus.idea.checkstyle.util;

import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;

import static com.intellij.notification.NotificationGroup.balloonGroup;
import static com.intellij.notification.NotificationGroup.logOnlyGroup;
import static com.intellij.notification.NotificationListener.URL_OPENING_LISTENER;
import static com.intellij.notification.NotificationType.*;
import static org.infernus.idea.checkstyle.CheckStyleBundle.message;
import static org.infernus.idea.checkstyle.util.Exceptions.rootCauseOf;

public final class Notifications {

    private static final NotificationGroup BALLOON_GROUP = balloonGroup(message("plugin.notification.alerts"));
    private static final NotificationGroup LOG_ONLY_GROUP = logOnlyGroup(message("plugin.notification.logging"));

    private Notifications() {
    }

    public static void showInfo(final Project project,
                                final String infoText) {
        BALLOON_GROUP
                .createNotification("", infoText, INFORMATION, URL_OPENING_LISTENER)
                .notify(project);
    }

    public static void showWarning(final Project project,
                                   final String warningText) {
        BALLOON_GROUP
                .createNotification("", warningText, WARNING, URL_OPENING_LISTENER)
                .notify(project);
    }

    public static void showError(final Project project,
                                 final String errorText) {
        BALLOON_GROUP
                .createNotification("", errorText, ERROR, URL_OPENING_LISTENER)
                .notify(project);
    }

    public static void showException(final Project project,
                                     final Throwable t) {
        LOG_ONLY_GROUP
                .createNotification(message("plugin.exception"), messageFor(t), ERROR, URL_OPENING_LISTENER)
                .notify(project);
    }

    @NotNull
    private static String messageFor(final Throwable t) {
        if (t.getCause() != null) {
            return message("checkstyle.exception-with-root-cause", t.getMessage(), traceOf(rootCauseOf(t)));
        }
        return message("checkstyle.exception", traceOf(t));
    }

    private static String traceOf(final Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return t.getMessage() + "<br>" + sw.toString()
                .replaceAll("\t", "&nbsp;&nbsp;")
                .replaceAll("\n", "<br>");
    }

}
