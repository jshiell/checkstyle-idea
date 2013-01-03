package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.checker.CheckerFactory;

/**
 * Clear the Checker cache, forcing a reload of rules files.
 */
public class ClearCheckerCache extends BaseAction {

    @Override
    public void actionPerformed(final AnActionEvent event) {
        final Project project = DataKeys.PROJECT.getData(event.getDataContext());
        ServiceManager.getService(project, CheckerFactory.class).invalidateCache();
    }
}
