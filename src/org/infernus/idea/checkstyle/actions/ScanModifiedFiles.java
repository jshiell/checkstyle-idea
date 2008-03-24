package org.infernus.idea.checkstyle.actions;

import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.project.Project;

/**
 * Scan modified files.
 * <p/>
 * If the project is not setup to use VCS then no files will be scanned.
 *
 * @author jgchristopher
 * @version 1.0
 */
public class ScanModifiedFiles extends BaseAction {

    /**
     * Logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(
            ScanModifiedFiles.class);

    /**
     * {@inheritDoc}
     */
    public final void actionPerformed(final AnActionEvent event) {
        try {
            final Project project = DataKeys.PROJECT.getData(event.getDataContext());
            if (project == null) {
                return;
            }
            ChangeListManager changeListManager = ChangeListManager.getInstance(project);
            project.getComponent(CheckStylePlugin.class).checkFiles(changeListManager.getAffectedFiles(), event);
        } catch (Throwable e) {
            final CheckStylePluginException processed = CheckStylePlugin.processError(null, e);
            if (processed != null) {
                LOG.error("Modified files scan failed", processed);
            }
        }
    }
}
