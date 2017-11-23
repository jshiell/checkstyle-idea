package org.infernus.idea.checkstyle.util;

import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.infernus.idea.checkstyle.CheckStyleBundle.message;
import static org.infernus.idea.checkstyle.util.Exceptions.rootCauseOf;

public final class Notifications {

    private static final NotificationGroup BALLOON_GROUP = NotificationGroup.balloonGroup("CheckStyle");
    private static final NotificationGroup LOG_ONLY_GROUP = NotificationGroup.logOnlyGroup("CheckStyle");

    private Notifications() {
    }

    public static void showWarning(final Project project,
                                   final String warningText) {
        BALLOON_GROUP
                .createNotification(warningText, NotificationType.WARNING)
                .notify(project);
    }

    public static void showError(final Project project,
                                 final String errorText) {
        BALLOON_GROUP
                .createNotification(errorText, NotificationType.ERROR)
                .notify(project);
    }

    public static void showException(final Project project,
                                     final Throwable t) {
        LOG_ONLY_GROUP
                .createNotification(messageFor(t), NotificationType.ERROR)
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
