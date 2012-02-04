package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.infernus.idea.checkstyle.checker.CheckerFactory;

/**
 * Clear the Checker cache, forcing a reload of rules files.
 */
public class ClearCheckerCache extends BaseAction {

    @Override
    public void actionPerformed(final AnActionEvent event) {
        CheckerFactory.getInstance().invalidateCache();
    }
}
