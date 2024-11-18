package org.infernus.idea.checkstyle.toolwindow;

import com.intellij.icons.AllIcons;
import org.infernus.idea.checkstyle.csapi.SeverityLevel;

import javax.swing.*;

public class SeverityGroupTreeInfo extends GroupTreeInfo {

    /**
     * Construct a severity node.
     *
     * @param severityLevel the severity level.
     * @param problemCount  the number of problems at this severity.
     */
    public SeverityGroupTreeInfo(final SeverityLevel severityLevel, final int problemCount) {
        super(severityLevel.name(), "file", iconForSeverity(severityLevel), problemCount);
    }

    private static Icon iconForSeverity(final SeverityLevel severityLevel) {
        return switch (severityLevel) {
            case Error -> AllIcons.General.Error;
            case Warning -> AllIcons.General.Warning;
            default -> AllIcons.General.Information;
        };
    }

}
