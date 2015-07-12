package org.infernus.idea.checkstyle.util;

import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

public final class Notifications {

    private static final NotificationGroup NOTIFICATION_GROUP = NotificationGroup.balloonGroup("CheckStyle");

    private Notifications() {
    }

    public static void showWarning(final Project project,
                                   final String warningText) {
        NOTIFICATION_GROUP
                .createNotification(warningText, NotificationType.WARNING)
                .notify(project);
    }

    public static void showError(final Project project,
                                 final String errorText) {
        NOTIFICATION_GROUP
                .createNotification(errorText, NotificationType.ERROR)
                .notify(project);
    }

}
