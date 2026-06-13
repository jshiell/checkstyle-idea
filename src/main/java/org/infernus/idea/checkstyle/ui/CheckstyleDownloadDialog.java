package org.infernus.idea.checkstyle.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.JBUI;
import org.infernus.idea.checkstyle.CheckstyleArtifactDownloader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.List;


/**
 * Modal dialog that downloads a Checkstyle version in the background and shows progress.
 * On failure, offers Retry / Use bundled version / Cancel options.
 */
public class CheckstyleDownloadDialog extends DialogWrapper {

    public static final int RESULT_USE_BUNDLED = NEXT_USER_EXIT_CODE;

    private final String version;
    private final CheckstyleArtifactDownloader downloader;

    private JPanel contentPanel;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JTextArea errorDetails;

    private volatile List<Path> downloadedPaths;
    private volatile boolean failed;

    public CheckstyleDownloadDialog(@Nullable final Project project,
                                    @NotNull final String version,
                                    @NotNull final CheckstyleArtifactDownloader downloader) {
        super(project, true);
        this.version = version;
        this.downloader = downloader;
        setTitle("Downloading Checkstyle " + version);
        setCancelButtonText("Cancel");
        init();
        startDownload();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        contentPanel = new JPanel(new BorderLayout(JBUI.scale(8), JBUI.scale(8)));
        contentPanel.setBorder(JBUI.Borders.empty(12));
        contentPanel.setPreferredSize(new Dimension(JBUI.scale(400), JBUI.scale(120)));

        statusLabel = new JLabel("Downloading Checkstyle " + version + " from Maven Central...");
        contentPanel.add(statusLabel, BorderLayout.NORTH);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        contentPanel.add(progressBar, BorderLayout.CENTER);

        errorDetails = new JTextArea();
        errorDetails.setEditable(false);
        errorDetails.setVisible(false);
        errorDetails.setLineWrap(true);
        errorDetails.setWrapStyleWord(true);
        contentPanel.add(new JScrollPane(errorDetails), BorderLayout.SOUTH);

        return contentPanel;
    }

    @Override
    @NotNull
    protected Action[] createActions() {
        return new Action[]{getCancelAction()};
    }

    private void startDownload() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                downloadedPaths = downloader.download(version);
                ApplicationManager.getApplication().invokeLater(this::onDownloadSuccess);
            } catch (Exception e) {
                failed = true;
                ApplicationManager.getApplication().invokeLater(() -> onDownloadFailure(e));
            }
        });
    }

    private void onDownloadSuccess() {
        close(OK_EXIT_CODE);
    }

    private void onDownloadFailure(@NotNull final Exception e) {
        statusLabel.setText("Failed to download Checkstyle " + version);
        progressBar.setVisible(false);
        errorDetails.setText(e.getMessage());
        errorDetails.setVisible(true);

        setOKButtonText("Retry");
        setOKActionEnabled(true);

        Action useBundledAction = new AbstractAction("Use bundled version") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                close(RESULT_USE_BUNDLED);
            }
        };

        getButton(getOKAction()).setVisible(true);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    @Override
    protected void doOKAction() {
        if (failed) {
            failed = false;
            statusLabel.setText("Downloading Checkstyle " + version + " from Maven Central...");
            progressBar.setVisible(true);
            errorDetails.setVisible(false);
            startDownload();
        } else {
            super.doOKAction();
        }
    }

    @Nullable
    public List<Path> getDownloadedPaths() {
        return downloadedPaths;
    }
}
