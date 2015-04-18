package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import org.infernus.idea.checkstyle.checker.CheckerFactoryCache;
import org.jetbrains.annotations.NotNull;

/**
 * Clear the Checker cache, forcing a reload of rules files.
 */
public class ClearCheckerCache extends BaseAction {

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        ServiceManager.getService(CheckerFactoryCache.class).invalidate();
    }
}
