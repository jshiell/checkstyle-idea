package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.checker.CheckerFactoryCache;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.components.ServiceManager.getService;

/**
 * Clear the Checker cache and blocks, forcing a reload of rules files.
 */
public class ResetLoadedRulesFiles extends BaseAction {

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        if (FileDocumentManager.getInstance().getUnsavedDocuments().length > 0) {
            // If there is an unsaved file, this could be the changed rules file or a dependent file.
            // Save them first, then continue.
            ApplicationManager.getApplication().runWriteAction(() -> {
                for (Document unsavedDocument : FileDocumentManager.getInstance().getUnsavedDocuments()) {
                    FileDocumentManager.getInstance().saveDocument(unsavedDocument);
                }
            });
        }

        project(event).ifPresent(project -> {
            getService(project, PluginConfigurationManager.class)
                    .getCurrent()
                    .getLocations()
                    .forEach(ConfigurationLocation::reset);
            getService(project, CheckerFactoryCache.class).invalidate();
        });
    }
}
