package org.infernus.idea.checkstyle.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.checker.Problem;
import org.infernus.idea.checkstyle.checks.Check;
import org.infernus.idea.checkstyle.csapi.Issue;
import org.infernus.idea.checkstyle.csapi.ProcessResultsThread;
import org.infernus.idea.checkstyle.csapi.SeverityLevel;
import org.jetbrains.annotations.NotNull;


public class CheckStyleAuditListener
        implements AuditListener {

    private static final Log LOG = LogFactory.getLog(CheckStyleAuditListener.class);

    private final boolean suppressErrors;
    private final List<Check> checks;
    private final int tabWidth;
    private final Optional<String> baseDir;
    private final Map<String, PsiFile> fileNamesToPsiFiles;

    private final List<Issue> errors = Collections.synchronizedList(new ArrayList<>());
    private Map<PsiFile, List<Problem>> problems = Collections.emptyMap();

    public CheckStyleAuditListener(@NotNull final Map<String, PsiFile> fileNamesToPsiFiles,
                                   final boolean suppressErrors,
                                   final int tabWidth,
                                   @NotNull final Optional<String> baseDir,
                                   @NotNull final List<Check> checks) {
        this.fileNamesToPsiFiles = new HashMap<>(fileNamesToPsiFiles);
        this.checks = checks;
        this.suppressErrors = suppressErrors;
        this.tabWidth = tabWidth;
        this.baseDir = baseDir;
    }


    public void auditStarted(final AuditEvent auditEvent) {
        errors.clear();
    }

    public void auditFinished(final AuditEvent auditEvent) {
        List<Issue> errorsCopy = null;
        synchronized (errors) {
            errorsCopy = new ArrayList<>(errors);
        }
        final ProcessResultsThread findThread = new ProcessResultsThread(suppressErrors, checks, tabWidth, baseDir,
                errorsCopy, fileNamesToPsiFiles);

        final Application application = ApplicationManager.getApplication();
        if (application != null) {  // can be null in unit tests
            // TODO Make sure that this does not block ... it seems that we are not starting a new thread.
            //      Sometimes, the editor is non-responsive because Checkstyle is still processing results.
            //      Problem: OpScan currently expects the ready-made list of problems synchronously.
            if (application.isDispatchThread()) {
                findThread.run();
            } else {
                application.runReadAction(findThread);
            }
            problems = findThread.getProblems();
        }
    }

    public void fileStarted(final AuditEvent auditEvent) {
        // do nothing
    }

    public void fileFinished(final AuditEvent auditEvent) {
        // do nothing
    }

    public void addError(final AuditEvent auditEvent) {
        errors.add(toIssue(auditEvent));
    }

    public void addException(final AuditEvent auditEvent, final Throwable throwable) {
        LOG.error("Exception during CheckStyle execution", throwable);
        errors.add(toIssue(auditEvent));
    }


    @NotNull
    public Map<PsiFile, List<Problem>> getProblems() {
        return problems;
    }


    private Issue toIssue(final AuditEvent auditEvent) {
        String msg = auditEvent.getMessage();
        if (auditEvent.getLocalizedMessage() != null) {
            msg = auditEvent.getLocalizedMessage().getMessage();
        }
        final SeverityLevel level = readSeverityLevel(auditEvent.getSeverityLevel());
        return new Issue(auditEvent.getFileName(), auditEvent.getLine(), auditEvent.getColumn(), msg, level,
                auditEvent.getSourceName());
    }


    private SeverityLevel readSeverityLevel(final com.puppycrawl.tools.checkstyle.api.SeverityLevel severityLevel) {
        SeverityLevel result = null;
        if (severityLevel != null) {
            switch (severityLevel) {
                case ERROR:
                    result = SeverityLevel.Error;
                    break;
                case WARNING:
                    result = SeverityLevel.Warning;
                    break;
                case INFO:
                    result = SeverityLevel.Info;
                    break;
                case IGNORE:
                    // fall through
                default:
                    result = SeverityLevel.Ignore;
                    break;
            }
        }
        return result;
    }
}
