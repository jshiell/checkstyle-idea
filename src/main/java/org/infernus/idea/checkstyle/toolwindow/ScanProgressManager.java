package org.infernus.idea.checkstyle.toolwindow;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static org.infernus.idea.checkstyle.util.Strings.isBlank;

/**
 * Manages the progress bar and progress label in the tool window panel.
 */
public class ScanProgressManager {

    private final JToolBar progressPanel;
    private final JProgressBar progressBar;
    private final JLabel progressLabel;

    public ScanProgressManager(@NotNull final JToolBar progressPanel,
                               @NotNull final JProgressBar progressBar,
                               @NotNull final JLabel progressLabel) {
        this.progressPanel = progressPanel;
        this.progressBar = progressBar;
        this.progressLabel = progressLabel;
    }

    /**
     * Update the progress text.
     *
     * @param text the new progress text, or null to clear.
     */
    public void setProgressText(@Nullable final String text) {
        if (isBlank(text)) {
            progressLabel.setText(" ");
        } else {
            progressLabel.setText(text);
        }
        progressLabel.validate();
    }

    /**
     * Show and reset the progress bar.
     */
    public void resetProgressBar() {
        progressBar.setValue(0);
        if (progressPanel.getComponentIndex(progressBar) == -1) {
            progressPanel.add(progressBar);
        }
        progressPanel.revalidate();
    }

    /**
     * Set the maximum limit, then show and reset the progress bar.
     *
     * @param max the maximum limit of the progress bar.
     */
    public void setProgressBarMax(final int max) {
        progressBar.setMaximum(max);
        resetProgressBar();
    }

    /**
     * Increment the progress bar value by the given amount.
     *
     * @param size the amount to increment by.
     */
    public void incrementProgressBarBy(final int size) {
        if (progressBar.getValue() < progressBar.getMaximum()) {
            progressBar.setValue(progressBar.getValue() + size);
        }
    }

    /**
     * Clear the progress bar and progress text.
     */
    public void clearProgress() {
        final int progressIndex = progressPanel.getComponentIndex(progressBar);
        if (progressIndex != -1) {
            progressPanel.remove(progressIndex);
            progressPanel.revalidate();
            progressPanel.repaint();
        }
        setProgressText(null);
    }
}
