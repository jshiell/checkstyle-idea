package org.infernus.idea.checkstyle.csapi;


/**
 * An issue as reported by the Checkstyle tool.
 */
public class Issue
{
    private final String fileName;

    private final int lineNo;

    private final int columnNo;

    private final String message;

    private final SeverityLevel severityLevel;

    private final String sourceName;


    public Issue(final String pFileName, final int pLineNo, final int pColumnNo, final String pMessage, final
    SeverityLevel pSeverityLevel, final String pSourceName) {
        fileName = pFileName;
        lineNo = pLineNo;
        columnNo = pColumnNo;
        message = pMessage;
        severityLevel = pSeverityLevel;
        sourceName = pSourceName;
    }


    public String getFileName() {
        return fileName;
    }

    public int getLineNo() {
        return lineNo;
    }

    public int getColumnNo() {
        return columnNo;
    }

    public String getMessage() {
        return message;
    }

    public SeverityLevel getSeverityLevel() {
        return severityLevel;
    }

    public String getSourceName() {
        return sourceName;
    }
}
