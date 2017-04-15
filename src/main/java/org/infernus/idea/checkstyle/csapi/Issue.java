package org.infernus.idea.checkstyle.csapi;


/**
 * An issue as reported by the Checkstyle tool.
 */
public class Issue {

    public final String fileName;
    public final int lineNumber;
    public final int columnNumber;
    public final String message;
    public final SeverityLevel severityLevel;
    public final String sourceName;

    public Issue(final String fileName,
                 final int lineNumber,
                 final int columnNumber,
                 final String message,
                 final SeverityLevel severityLevel,
                 final String sourceName) {
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.message = message;
        this.severityLevel = severityLevel;
        this.sourceName = sourceName;
    }

}
