package org.infernus.idea.checkstyle.handlers;

import com.intellij.CommonBundle;
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
import org.infernus.idea.checkstyle.StaticScanner;
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
import static java.util.Optional.ofNullable;
import static org.infernus.idea.checkstyle.CheckStyleBundle.message;

public class ScanFilesBeforeCheckinHandler extends CheckinHandler {
    private static final Logger LOG = Logger.getInstance(ScanFilesBeforeCheckinHandler.class);
    public static final String DOT_SUFFIX = "...";

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

        final StaticScanner staticScanner = project.getService(StaticScanner.class);
        if (staticScanner == null) {
            LOG.warn("Could not get scanner, skipping");
            return COMMIT;
        }

        if (configurationManager(project).getCurrent().isScanBeforeCheckin()) {
            try {
                final Map<PsiFile, List<Problem>> scanResults = new HashMap<>();
                new Task.Modal(project, message("handler.before.checkin.scan.text"), false) {
                    public void run(@NotNull final ProgressIndicator progressIndicator) {
                        progressIndicator.setText(message("handler.before.checkin.scan.in-progress"));
                        progressIndicator.setIndeterminate(true);
                        scanResults.putAll(staticScanner.scanFiles(new ArrayList<>(checkinPanel.getVirtualFiles())));
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
        return ofNullable(configurationManager(project));
    }

    private ReturnResult processScanResults(final Map<PsiFile, List<Problem>> results,
                                            final CommitExecutor executor,
                                            final Project project) {
        final long errorCount = countOf(results, SeverityLevel.Error);
        final long warningCount = countOf(results, SeverityLevel.Warning);
        if (errorCount == 0 && warningCount == 0) {
            return COMMIT;
        }

        final int answer = promptUser(project, errorCount, warningCount, executor);
        if (answer == Messages.OK) {
            showResultsInToolWindow(results, project);
            return CLOSE_WINDOW;

        } else if (answer == Messages.CANCEL || answer < 0) {
            return CANCEL;
        }

        return COMMIT;
    }

    private long countOf(final Map<PsiFile, List<Problem>> results, final SeverityLevel level) {
        return results.entrySet().stream()
                .map(entry -> problemsAtLevel(entry, level))
                .mapToLong(Long::longValue)
                .sum();
    }

    private long problemsAtLevel(final Map.Entry<PsiFile, List<Problem>> entry, final SeverityLevel level) {
        return entry.getValue()
                .stream()
                .filter(problem -> problem.severityLevel() == level)
                .count();
    }

    private int promptUser(final Project project,
                           final long errorCount,
                           final long warningCount,
                           final CommitExecutor executor) {
        String commitButtonText;
        if (executor != null) {
            commitButtonText = executor.getActionText();
        } else {
            commitButtonText = checkinPanel.getCommitActionName();
        }

        if (commitButtonText.endsWith(DOT_SUFFIX)) {
            commitButtonText = commitButtonText.substring(0, commitButtonText.length() - DOT_SUFFIX.length());
        }

        final String[] buttons = new String[]{
                message("handler.before.checkin.error.review"),
                commitButtonText,
                CommonBundle.getCancelButtonText()};

        return Messages.showDialog(project, message("handler.before.checkin.error.text", errorCount, warningCount),
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
        return project.getService(PluginConfigurationManager.class);
    }

}
