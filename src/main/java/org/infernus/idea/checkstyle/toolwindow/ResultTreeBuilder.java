package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import org.infernus.idea.checkstyle.csapi.SeverityLevel;
import org.infernus.idea.checkstyle.exception.CheckStylePluginParseException;
import org.infernus.idea.checkstyle.exception.CheckstyleToolException;
import org.infernus.idea.checkstyle.model.ScanResult;

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
        treeModel.clear();
        treeModel.setRootMessage("plugin.results.in-progress");
    }

    public void displayWarningResult(final String messageKey, final Object... messageArgs) {
        progressManager.clearProgress();
        treeModel.clear();
        treeModel.setRootMessage(messageKey, messageArgs);
    }

    /**
     * Display the passed results.
     *
     * @param scanResults    the results of the scan.
     * @param warningMessage a warning message to display about the results, if appropriate.
     */
    public void displayResults(final List<ScanResult> scanResults, final String warningMessage) {
        treeModel.setModel(scanResults, getDisplayedSeverities());
        progressManager.clearProgress();
        if (warningMessage != null) {
            progressManager.setProgressText(warningMessage);
        }
        navigator.expandTree(treeModel, 3);
    }

    /**
     * Clear the results and display notice to say an error occurred.
     *
     * @param error the error that occurred.
     */
    public void displayErrorResult(final Throwable error) {
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
            } else {
                errorText = message("plugin.results.error");
            }
        }
        treeModel.clear();
        treeModel.setRootText(errorText);
        progressManager.clearProgress();
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
