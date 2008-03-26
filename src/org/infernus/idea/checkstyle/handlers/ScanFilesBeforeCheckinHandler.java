package org.infernus.idea.checkstyle.handlers;

import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitExecutor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.CommonBundle;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;

import org.jetbrains.annotations.Nullable;
import org.infernus.idea.checkstyle.CheckStyleConfiguration;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.CheckStyleConstants;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Before Checkin Handler to scan files with Checkstyle.
 */
public class ScanFilesBeforeCheckinHandler extends CheckinHandler {

    private final CheckStylePlugin plugin;
    private final CheckinProjectPanel checkinPanel;

    /**
     * Checkstyle before checkin handler. 
     * @param plugin Checkstyle plugin reference
     * @param myCheckinPanel checkin project panel
     */
    public ScanFilesBeforeCheckinHandler(final CheckStylePlugin plugin, CheckinProjectPanel myCheckinPanel) {
        this.plugin = plugin;
        this.checkinPanel = myCheckinPanel;
    }

    @Nullable
    public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
        final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);
        final JCheckBox checkBox = new JCheckBox(resources.getString("handler.before.checkin.checkbox"));
        return new RefreshableOnComponent() {
            public JComponent getComponent() {
                JPanel panel = new JPanel(new BorderLayout());
                panel.add(checkBox);
                return panel;
            }

            public void refresh() {
            }

            public void saveState() {
                getSettings().SCAN_FILES_BEFORE_CHECKIN = checkBox.isSelected();
            }

            public void restoreState() {
                checkBox.setSelected(getSettings().SCAN_FILES_BEFORE_CHECKIN);
            }
        };
    }

    /**
     * Run Check if selected. 
     * @param commitExecutor commit executor
     * @return ReturnResult
     */
    public ReturnResult beforeCheckin(@Nullable CommitExecutor commitExecutor) {
        if (getSettings().SCAN_FILES_BEFORE_CHECKIN) {
            try {
                final Map<PsiFile, java.util.List<ProblemDescriptor>> scanResults = new HashMap<PsiFile, List<ProblemDescriptor>>();
                new Task.Modal(this.plugin.getProject(), "CheckStyle is Scanning", false) {
                    public void run(ProgressIndicator progressIndicator) {
                        progressIndicator.setText("Scanning ...");
                        progressIndicator.setIndeterminate(true);
                        ScanFilesBeforeCheckinHandler.this.plugin.scanFiles(new ArrayList<VirtualFile>(checkinPanel.getVirtualFiles()),scanResults);
                    }
                }.queue();

                if (!scanResults.isEmpty()) {
                    return processScanResults(scanResults, commitExecutor);
                } else {
                    return ReturnResult.COMMIT;
                }
            }
            catch (ProcessCanceledException e) {
                return ReturnResult.CANCEL;
            }

        } else {
            return ReturnResult.COMMIT;
        }
    }

    /**
     * Get plugin configuration.
     * @return CheckStyleConfiguration plugin configuration
     */
    private CheckStyleConfiguration getSettings() {
        return this.plugin.getConfiguration();
    }

    /**
     * Process scan results and allow user to decide what to do.
     * @param results scan results.
     * @param executor commit executor
     * @return ReturnResult Users decision.
     */
    private ReturnResult processScanResults(Map<PsiFile, java.util.List<ProblemDescriptor>> results, CommitExecutor executor) {
        int errorCount = results.keySet().size();
        String commitButtonText = executor != null ? executor.getActionText() : checkinPanel.getCommitActionName();
        if (commitButtonText.endsWith("...")) {
            commitButtonText = commitButtonText.substring(0, commitButtonText.length() - 3);
        }

        final int answer = Messages.showDialog(errorCount + " Files contain problems",
                "CheckStyle Scan", new String[]{"Review",
                commitButtonText, CommonBundle.getCancelButtonText()}, 0, UIUtil.getWarningIcon());
        if (answer == 0) {
            this.plugin.getToolWindowPanel().displayResults(results);
            this.plugin.getToolWindowPanel().expandTree();
            this.plugin.activeToolWindow(true);
            return ReturnResult.CLOSE_WINDOW;
        } else if (answer == 2 || answer == -1) {
            return ReturnResult.CANCEL;
        } else {
            return ReturnResult.COMMIT;
        }
    }


}
