package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.csapi.SeverityLevel;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.exception.CheckStylePluginParseException;
import org.infernus.idea.checkstyle.exception.CheckstyleToolException;
import org.infernus.idea.checkstyle.model.ScanResult;
import org.infernus.idea.checkstyle.model.ScanResultMerger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.infernus.idea.checkstyle.CheckStyleBundle.message;

/**
 * Manages result tree model: populating results, filtering by severity, and grouping.
 */
public class ResultTreeBuilder {

    private static final Logger LOG = Logger.getInstance(ResultTreeBuilder.class);

    private static final Map<Pattern, String> CHECKSTYLE_ERROR_PATTERNS = new HashMap<>();

    static {
        try {
            CHECKSTYLE_ERROR_PATTERNS.put(
                    Pattern.compile("Property \\$\\{([^}]*)} has not been set"),
                    "plugin.results.error.missing-property");
            CHECKSTYLE_ERROR_PATTERNS.put(
                    Pattern.compile("Unable to instantiate (.*)"),
                    "plugin.results.error.instantiation-failed");
        } catch (Throwable t) {
            LOG.warn("Pattern mappings could not be instantiated.", t);
        }
    }

    private final ResultTreeModel treeModel;
    private final ScanProgressManager progressManager;
    private final ResultTreeNavigator navigator;

    private boolean displayingErrors = true;
    private boolean displayingWarnings = true;
    private boolean displayingInfo = true;

    private List<ScanResult> lastScanResults = List.of();

    public ResultTreeBuilder(final ResultTreeModel treeModel,
                             final ScanProgressManager progressManager,
                             final ResultTreeNavigator navigator) {
        this.treeModel = treeModel;
        this.progressManager = progressManager;
        this.navigator = navigator;
    }

    public Set<SeverityLevel> getDisplayedSeverities() {
        final Set<SeverityLevel> severityLevels = new HashSet<>();
        if (displayingErrors) {
            severityLevels.add(SeverityLevel.Error);
        }
        if (displayingWarnings) {
            severityLevels.add(SeverityLevel.Warning);
        }
        if (displayingInfo) {
            severityLevels.add(SeverityLevel.Info);
        }
        return severityLevels;
    }

    /**
     * Clear the results and display a 'scan in progress' notice.
     *
     * @param size the number of files being scanned.
     */
    public void displayInProgress(final int size) {
        progressManager.setProgressBarMax(size);
        lastScanResults = List.of();
        treeModel.clear();
        treeModel.setRootMessage("plugin.results.in-progress");
    }

    public void displayWarningResult(final String messageKey, final Object... messageArgs) {
        progressManager.clearProgress();
        lastScanResults = List.of();
        treeModel.clear();
        treeModel.setRootMessage(messageKey, messageArgs);
    }

    /**
     * The results currently on display, as a basis for merging a re-scan into.
     *
     * @return the displayed results, which is empty whenever the tree has been cleared.
     */
    List<ScanResult> lastScanResults() {
        return lastScanResults;
    }

    /**
     * Is there anything on display worth re-scanning?
     *
     * @return true if any problems are displayed.
     */
    public boolean hasResults() {
        // ignored problems are never rendered, so a tree holding only those has nothing to refresh
        return lastScanResults.stream()
                .flatMap(scanResult -> scanResult.problems().values().stream())
                .flatMap(List::stream)
                .anyMatch(problem -> problem.severityLevel() != SeverityLevel.Ignore);
    }

    /**
     * Discard the results for the passed files.
     * <p>
     * A file that has been deleted since it was scanned is never sent to a re-scan, so it would
     * otherwise linger in the tree forever.
     *
     * @param files the files to discard the results for.
     */
    public void discardResultsFor(final Set<PsiFile> files) {
        if (files.isEmpty()) {
            return;
        }

        lastScanResults = lastScanResults.stream()
                .map(scanResult -> withoutProblemsFor(scanResult, files))
                .filter(scanResult -> !scanResult.problems().isEmpty())
                .toList();

        treeModel.setModel(lastScanResults, getDisplayedSeverities());
        navigator.expandTree(treeModel, 3);
    }

    private ScanResult withoutProblemsFor(final ScanResult scanResult, final Set<PsiFile> files) {
        final var retained = new HashMap<>(scanResult.problems());
        retained.keySet().removeAll(files);
        return new ScanResult(scanResult.configurationLocationResult(), scanResult.module(),
                retained, scanResult.scannedFiles());
    }

    /**
     * Display the passed results.
     *
     * @param scanResults    the results of the scan.
     * @param warningMessage a warning message to display about the results, if appropriate.
     */
    public void displayResults(final List<ScanResult> scanResults, final String warningMessage) {
        lastScanResults = resultsWorthDisplaying(scanResults);
        treeModel.setModel(lastScanResults, getDisplayedSeverities());
        progressManager.clearProgress();
        if (warningMessage != null) {
            progressManager.setProgressText(warningMessage);
        }
        navigator.expandTree(treeModel, 3);
    }

    /**
     * Merge the passed results of a re-scan into the results already on display.
     *
     * @param scanResults    the results of the re-scan.
     * @param warningMessage a warning message to display about the results, if appropriate.
     */
    public void mergeResults(final List<ScanResult> scanResults, final String warningMessage) {
        lastScanResults = ScanResultMerger.merge(lastScanResults, resultsWorthDisplaying(scanResults));
        treeModel.setModel(lastScanResults, getDisplayedSeverities());
        progressManager.clearProgress();
        if (warningMessage != null) {
            progressManager.setProgressText(warningMessage);
        }
        navigator.expandTree(treeModel, 3);
    }

    /**
     * Display a 'scan in progress' notice without disturbing the results already on display, so that
     * they remain readable while the re-scan runs.
     *
     * @param size the number of files being re-scanned.
     */
    public void displayRefreshInProgress(final int size) {
        progressManager.setProgressBarMax(size);
    }

    /**
     * Discard the results that say nothing about any file, so that whatever a caller hands us can
     * safely be retained and merged against later.
     */
    private List<ScanResult> resultsWorthDisplaying(final List<ScanResult> scanResults) {
        return scanResults.stream()
                .filter(scanResult -> scanResult.configurationLocationResult() != null)
                .filter(scanResult -> switch (scanResult.configurationLocationResult().status()) {
                    case NOT_PRESENT, BLOCKED -> false;
                    default -> true;
                })
                .toList();
    }

    /**
     * Clear the results and display notice to say an error occurred.
     *
     * @param error the error that occurred.
     */
    public void displayErrorResult(final Throwable error) {
        lastScanResults = List.of();
        treeModel.clear();
        treeModel.setRootText(messageFor(error));
        progressManager.clearProgress();
    }

    /**
     * Describe an error to the user, in as much detail as we can extract from it.
     *
     * @param error the error that occurred.
     * @return the message to display.
     */
    public static String messageFor(final Throwable error) {
        String errorText = null;
        if (error instanceof CheckstyleToolException && error.getCause() != null) {
            for (final Map.Entry<Pattern, String> errorPatternEntry : CHECKSTYLE_ERROR_PATTERNS.entrySet()) {
                final Matcher errorMatcher = errorPatternEntry.getKey().matcher(error.getCause().getMessage());
                if (errorMatcher.find()) {
                    final Object[] args = new Object[errorMatcher.groupCount()];
                    for (int i = 0; i < errorMatcher.groupCount(); ++i) {
                        args[i] = errorMatcher.group(i + 1);
                    }
                    errorText = message(errorPatternEntry.getValue(), args);
                }
            }
        }
        if (errorText == null) {
            if (error instanceof CheckStylePluginParseException) {
                errorText = message("plugin.results.unparseable");
            } else if (error instanceof CheckStylePluginException pluginException
                    && !(error instanceof CheckstyleToolException)
                    && pluginException.getMessage() != null
                    && !pluginException.getMessage().isBlank()) {
                errorText = message("plugin.results.error.detail", pluginException.getMessage());
            } else {
                errorText = message("plugin.results.error");
            }
        }
        return errorText;
    }

    /**
     * Refresh the displayed results based on the current filter settings.
     */
    public void filterDisplayedResults() {
        treeModel.filter(getDisplayedSeverities());
        navigator.expandTree(treeModel, 3);
    }

    public boolean isDisplayingErrors() {
        return displayingErrors;
    }

    public void setDisplayingErrors(final boolean displayingErrors) {
        this.displayingErrors = displayingErrors;
    }

    public boolean isDisplayingWarnings() {
        return displayingWarnings;
    }

    public void setDisplayingWarnings(final boolean displayingWarnings) {
        this.displayingWarnings = displayingWarnings;
    }

    public boolean isDisplayingInfo() {
        return displayingInfo;
    }

    public void setDisplayingInfo(final boolean displayingInfo) {
        this.displayingInfo = displayingInfo;
    }

    public void groupBy(final ResultGrouping grouping) {
        treeModel.groupBy(grouping);
    }

    public ResultGrouping groupedBy() {
        return treeModel.groupedBy();
    }
}
