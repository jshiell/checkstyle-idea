package org.infernus.idea.checkstyle.util;

import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.exception.CheckStylePluginParseException;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;

import static com.intellij.notification.NotificationListener.URL_OPENING_LISTENER;
import static com.intellij.notification.NotificationType.*;
import static org.infernus.idea.checkstyle.CheckStyleBundle.message;
import static org.infernus.idea.checkstyle.util.Exceptions.rootCauseOf;

public final class Notifications {

    private static final NotificationGroup BALLOON_GROUP =
            NotificationGroupManager.getInstance().getNotificationGroup("CheckStyleIDEABalloonGroup");
    private static final NotificationGroup LOG_ONLY_GROUP =
            NotificationGroupManager.getInstance().getNotificationGroup("CheckStyleIDEALogOnlyGroup");

    private Notifications() {
    }

    public static void showInfo(final Project project,
                                final String infoText) {
        BALLOON_GROUP
                .createNotification("", infoText, INFORMATION)
                .setListener(URL_OPENING_LISTENER)
                .notify(project);
    }

    public static void showWarning(final Project project,
                                   final String warningText) {
        BALLOON_GROUP
                .createNotification("", warningText, WARNING)
                .setListener(URL_OPENING_LISTENER)
                .notify(project);
    }

    public static void showError(final Project project,
                                 final String errorText) {
        BALLOON_GROUP
                .createNotification("", errorText, ERROR)
                .setListener(URL_OPENING_LISTENER)
                .notify(project);
    }

    public static void showException(final Project project,
                                     final Throwable t) {
        LOG_ONLY_GROUP
                .createNotification(titleFor(t), messageFor(t), ERROR)
                .setListener(URL_OPENING_LISTENER)
                .notify(project);
    }

    @NotNull
    private static String titleFor(final Throwable t) {
        if (isParseException(t)) {
            return message("plugin.exception.parse");
        }
        return message("plugin.exception");
    }

    private static boolean isParseException(final Throwable t) {
        return t instanceof CheckStylePluginParseException;
    }

    @NotNull
    private static String messageFor(final Throwable t) {
        String detailSuffix = detailSuffixOf(t);
        if (t.getCause() != null) {
            return message("checkstyle.exception-with-root-cause" + detailSuffix, t.getMessage(), traceOf(rootCauseOf(t)));
        }
        return message("checkstyle.exception" + detailSuffix, traceOf(t));
    }

    @NotNull
    private static String detailSuffixOf(final Throwable t) {
        if (isParseException(t)) {
            return  ".parse";
        }
        return "";
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
