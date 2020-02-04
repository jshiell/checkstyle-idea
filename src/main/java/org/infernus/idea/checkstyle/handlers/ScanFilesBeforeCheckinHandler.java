package org.infernus.idea.checkstyle.handlers;

import com.intellij.CommonBundle;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitExecutor;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.psi.PsiFile;
import com.intellij.util.PairConsumer;
import com.intellij.util.ui.UIUtil;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.checker.Problem;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.csapi.SeverityLevel;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

import static com.intellij.openapi.vcs.checkin.CheckinHandler.ReturnResult.*;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.infernus.idea.checkstyle.CheckStyleBundle.message;

public class ScanFilesBeforeCheckinHandler extends CheckinHandler {
    private static final Logger LOG = Logger.getInstance(ScanFilesBeforeCheckinHandler.class);

    private final CheckinProjectPanel checkinPanel;

    public ScanFilesBeforeCheckinHandler(@NotNull final CheckinProjectPanel myCheckinPanel) {
        this.checkinPanel = myCheckinPanel;
    }

    @Nullable
    public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
        final JCheckBox checkBox = new JCheckBox(message("handler.before.checkin.checkbox"));

        return new RefreshableOnComponent() {
            public JComponent getComponent() {
                final JPanel panel = new JPanel(new BorderLayout());
                panel.add(checkBox);
                return panel;
            }

            public void refresh() {
            }

            public void saveState() {
                settings().ifPresent(settings -> settings.setCurrent(
                        PluginConfigurationBuilder.from(settings.getCurrent()).withScanBeforeCheckin(checkBox.isSelected()).build(),
                        false));
            }

            public void restoreState() {
                checkBox.setSelected(
                        settings().map(c -> c.getCurrent().isScanBeforeCheckin())
                                .orElse(Boolean.FALSE));
            }
        };
    }

    @Override
    public ReturnResult beforeCheckin(@Nullable final CommitExecutor executor,
                                      final PairConsumer<Object, Object> additionalDataConsumer) {
        final Project project = checkinPanel.getProject();
        if (project == null) {
            LOG.warn("Could not get project for check-in panel, skipping");
            return COMMIT;
        }

        final CheckStylePlugin plugin = ServiceManager.getService(project, CheckStylePlugin.class);
        if (plugin == null) {
            LOG.warn("Could not get CheckStyle Plug-in, skipping");
            return COMMIT;
        }

        if (configurationManager(project).getCurrent().isScanBeforeCheckin()) {
            try {
                final Map<PsiFile, List<Problem>> scanResults = new HashMap<>();
                new Task.Modal(project, message("handler.before.checkin.scan.text"), false) {
                    public void run(@NotNull final ProgressIndicator progressIndicator) {
                        progressIndicator.setText(message("handler.before.checkin.scan.in-progress"));
                        progressIndicator.setIndeterminate(true);
                        scanResults.putAll(plugin.scanFiles(new ArrayList<>(checkinPanel.getVirtualFiles())));
                    }
                }.queue();

                return processScanResults(scanResults, executor, project);

            } catch (ProcessCanceledException e) {
                return CANCEL;
            }

        } else {
            return COMMIT;
        }
    }

    private Optional<PluginConfigurationManager> settings() {
        final Project project = checkinPanel.getProject();
        if (project == null) {
            LOG.warn("Could not get project for check-in panel");
            return empty();
        }

        return ofNullable(configurationManager(project));
    }

    private ReturnResult processScanResults(final Map<PsiFile, List<Problem>> results,
                                            final CommitExecutor executor,
                                            final Project project) {
        final int errorCount = errorCountOf(results);
        if (errorCount == 0) {
            return COMMIT;
        }

        final int answer = promptUser(project, errorCount, executor);
        if (answer == Messages.OK) {
            showResultsInToolWindow(results, project);
            return CLOSE_WINDOW;

        } else if (answer == Messages.CANCEL || answer < 0) {
            return CANCEL;
        }

        return COMMIT;
    }

    private int errorCountOf(final Map<PsiFile, List<Problem>> results) {
        return (int) results.entrySet().stream()
                .filter(this::hasProblemsThatAreNotIgnored)
                .count();
    }

    private boolean hasProblemsThatAreNotIgnored(final Map.Entry<PsiFile, List<Problem>> entry) {
        return entry.getValue()
                .stream()
                .anyMatch(problem -> problem.severityLevel() != SeverityLevel.Ignore);
    }

    private int promptUser(final Project project,
                           final int errorCount,
                           final CommitExecutor executor) {
        String commitButtonText;
        if (executor != null) {
            commitButtonText = executor.getActionText();
        } else {
            commitButtonText = checkinPanel.getCommitActionName();
        }

        if (commitButtonText.endsWith("...")) {
            commitButtonText = commitButtonText.substring(0, commitButtonText.length() - 3);
        }

        final String[] buttons = new String[]{
                message("handler.before.checkin.error.review"),
                commitButtonText,
                CommonBundle.getCancelButtonText()};

        return Messages.showDialog(project, message("handler.before.checkin.error.text", errorCount),
                message("handler.before.checkin.error.title"),
                buttons, 0, UIUtil.getWarningIcon());
    }

    private void showResultsInToolWindow(final Map<PsiFile, List<Problem>> results,
                                         final Project project) {
        final CheckStyleToolWindowPanel toolWindowPanel = CheckStyleToolWindowPanel.panelFor(project);
        if (toolWindowPanel != null) {
            toolWindowPanel.displayResults(results);
            toolWindowPanel.showToolWindow();
        }
    }

    private PluginConfigurationManager configurationManager(final Project project) {
        return ServiceManager.getService(project, PluginConfigurationManager.class);
    }

}
