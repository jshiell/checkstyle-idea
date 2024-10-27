package org.infernus.idea.checkstyle.util;

import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.exception.CheckStylePluginParseException;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;

import static com.intellij.notification.NotificationType.*;
import static org.infernus.idea.checkstyle.CheckStyleBundle.message;
import static org.infernus.idea.checkstyle.util.Exceptions.rootCauseOf;

public final class Notifications {

    private Notifications() {
    }

    public static void showInfo(final Project project,
                                final String infoText,
                                final NotificationAction action) {
        balloonGroup()
                .createNotification("", infoText, INFORMATION)
                .addAction(action)
                .notify(project);
    }

    public static void showWarning(final Project project,
                                   final String warningText) {
        balloonGroup()
                .createNotification("", warningText, WARNING)
                .notify(project);
    }

    public static void showError(final Project project,
                                 final String errorText) {
        balloonGroup()
                .createNotification("", errorText, ERROR)
                .notify(project);
    }

    public static void showException(final Project project,
                                     final Throwable t) {
        logOnlyGroup()
                .createNotification(titleFor(t), messageFor(t), ERROR)
                .notify(project);
    }

    private static NotificationGroup balloonGroup() {
        return NotificationGroupManager.getInstance()
                .getNotificationGroup("CheckStyleIDEABalloonGroup");
    }

    private static NotificationGroup logOnlyGroup() {
        return NotificationGroupManager.getInstance()
                .getNotificationGroup("CheckStyleIDEALogOnlyGroup");
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
            return ".parse";
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
