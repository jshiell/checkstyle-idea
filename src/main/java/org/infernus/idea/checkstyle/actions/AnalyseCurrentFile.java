package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE;

/**
 * Action to execute a CheckStyle scan on the current editor file.
 */
public class AnalyseCurrentFile extends ScanCurrentFile {

    @Override
    protected @Nullable VirtualFile selectedFile(@NotNull final Project project,
                                                 @NotNull final AnActionEvent event) {
        return event.getDataContext().getData(VIRTUAL_FILE);
    }

    @Override
    public void update(@NotNull final AnActionEvent event) {
        // update event does not expose selected file, so we always enable
        final Presentation presentation = event.getPresentation();
        project(event).ifPresentOrElse(
                project -> presentation.setEnabled(!staticScanner(project).isScanInProgress()),
                () -> presentation.setEnabled(false));
    }
}
