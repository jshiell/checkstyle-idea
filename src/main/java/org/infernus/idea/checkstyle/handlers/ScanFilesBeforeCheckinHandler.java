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
import org.infernus.idea.checkstyle.CheckStyleConfiguration;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.checker.Problem;
import org.infernus.idea.checkstyle.config.PluginConfigDtoBuilder;
import org.infernus.idea.checkstyle.csapi.SeverityLevel;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import static com.intellij.openapi.vcs.checkin.CheckinHandler.ReturnResult.*;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
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
                        PluginConfigDtoBuilder.from(settings.getCurrent()).withScanBeforeCheckin(checkBox.isSelected()).build(),
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

        final CheckStylePlugin plugin = project.getComponent(CheckStylePlugin.class);
        if (plugin == null) {
            LOG.warn("Could not get CheckStyle Plug-in, skipping");
            return COMMIT;
        }

        if (plugin.getConfiguration().getCurrent().isScanBeforeCheckin()) {
            try {
                final Map<PsiFile, List<Problem>> scanResults = new HashMap<>();
                new Task.Modal(project, message("handler.before.checkin.scan.text"), false) {
                    public void run(@NotNull final ProgressIndicator progressIndicator) {
                        progressIndicator.setText(message("handler.before.checkin.scan.in-progress"));
                        progressIndicator.setIndeterminate(true);
                        scanResults.putAll(plugin.scanFiles(new ArrayList<>(checkinPanel.getVirtualFiles())));
                    }
                }.queue();

                return processScanResults(scanResults, executor, plugin);

            } catch (ProcessCanceledException e) {
                return CANCEL;
            }

        } else {
            return COMMIT;
        }
    }

    private Optional<CheckStyleConfiguration> settings() {
        final Project project = checkinPanel.getProject();
        if (project == null) {
            LOG.warn("Could not get project for check-in panel");
            return empty();
        }

        final CheckStylePlugin plugin = project.getComponent(CheckStylePlugin.class);
        if (plugin == null) {
            LOG.warn("Could not get CheckStyle Plug-in, skipping");
            return empty();
        }

        return ofNullable(plugin.getConfiguration());
    }

    private ReturnResult processScanResults(final Map<PsiFile, List<Problem>> results,
                                            final CommitExecutor executor,
                                            final CheckStylePlugin plugin) {
        final int errorCount = errorCountOf(results);
        if (errorCount == 0) {
            return COMMIT;
        }

        final int answer = promptUser(plugin, errorCount, executor);
        if (answer == Messages.OK) {
            showResultsInToolWindow(results, plugin);
            return CLOSE_WINDOW;

        } else if (answer == Messages.CANCEL || answer < 0) {
            return CANCEL;
        }

        return COMMIT;
    }

    private int errorCountOf(final Map<PsiFile, List<Problem>> results) {
        return results.entrySet().stream()
                .filter(this::hasProblemsThatAreNotIgnored)
                .collect(toList())
                .size();
    }

    private boolean hasProblemsThatAreNotIgnored(final Map.Entry<PsiFile, List<Problem>> entry) {
        return entry.getValue()
                .stream()
                .filter(problem -> problem.severityLevel() != SeverityLevel.Ignore)
                .collect(toList())
                .size() > 0;
    }

    private int promptUser(final CheckStylePlugin plugin,
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

        return Messages.showDialog(plugin.getProject(), message("handler.before.checkin.error.text", errorCount),
                message("handler.before.checkin.error.title"),
                buttons, 0, UIUtil.getWarningIcon());
    }

    private void showResultsInToolWindow(final Map<PsiFile, List<Problem>> results,
                                         final CheckStylePlugin plugin) {
        final CheckStyleToolWindowPanel toolWindowPanel = CheckStyleToolWindowPanel.panelFor(plugin.getProject());
        if (toolWindowPanel != null) {
            toolWindowPanel.displayResults(results);
            toolWindowPanel.showToolWindow();
        }
    }

}
