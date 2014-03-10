package org.infernus.idea.checkstyle.checker;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.checks.Check;
import org.infernus.idea.checkstyle.util.ExtendedProblemDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Listener for the CheckStyle process.
 */
public class CheckStyleAuditListener implements AuditListener {

    private static final Log LOG = LogFactory.getLog(CheckStyleAuditListener.class);

    private final boolean usingExtendedDescriptors;
    private final boolean suppressErrors;
    private final List<Check> checks;
    private final int tabWidth;
    private final Map<String, PsiFile> fileNamesToPsiFiles;
    private final InspectionManager manager;

    private final List<AuditEvent> errors = new ArrayList<AuditEvent>();
    private final Map<PsiFile, List<ProblemDescriptor>> problems = new HashMap<PsiFile, List<ProblemDescriptor>>();

    /**
     * Create a new listener.
     * <p/>
     * Use the second argument to determine if we return our extended problem
     * descriptors. This is provided to avoid problems with downstream code
     * that may be interested in the implementation type.
     *
     * @param fileNamesToPsiFiles    a map of files name to PSI files for the files being scanned.
     * @param manager                the current inspection manager.
     * @param useExtendedDescriptors should we return standard IntelliJ
 *                               problem descriptors or extended ones with severity information?
     * @param suppressErrors         pass CheckStyle errors to IDEA as warnings.
     * @param tabWidth               the tab width for the given rules file.
     * @param checks                 the check modifications to use.
     */
    public CheckStyleAuditListener(final Map<String, PsiFile> fileNamesToPsiFiles,
                                   final InspectionManager manager,
                                   final boolean useExtendedDescriptors,
                                   final boolean suppressErrors,
                                   final int tabWidth, final List<Check> checks) {
        this.fileNamesToPsiFiles = new HashMap<String, PsiFile>(fileNamesToPsiFiles);
        this.manager = manager;
        this.usingExtendedDescriptors = useExtendedDescriptors;
        this.checks = checks;
        this.suppressErrors = suppressErrors;
        this.tabWidth = tabWidth;
    }

    public void auditStarted(final AuditEvent auditEvent) {
        synchronized (errors) {
            errors.clear();
            problems.clear();
        }
    }

    public void auditFinished(final AuditEvent auditEvent) {
        final ProcessResultsThread findThread = new ProcessResultsThread();

        final Application application = ApplicationManager.getApplication();
        if (application.isDispatchThread()) {
            findThread.run();
        } else {
            application.runReadAction(findThread);
        }
    }

    public void fileStarted(final AuditEvent auditEvent) {

    }

    public void fileFinished(final AuditEvent auditEvent) {

    }

    public void addError(final AuditEvent auditEvent) {
        synchronized (errors) {
            errors.add(auditEvent);
        }
    }

    public void addException(final AuditEvent auditEvent,
                             final Throwable throwable) {
        LOG.error("Exception during CheckStyle execution", throwable);
        synchronized (errors) {
            errors.add(auditEvent);
        }
    }

    /**
     * Get the problems for a given file as found by this scan.
     *
     * @param psiFile the file to check for results.
     * @return the problems found by this scan.
     */
    public List<ProblemDescriptor> getProblems(final PsiFile psiFile) {
        final List<ProblemDescriptor> problemsForFile = problems.get(psiFile);
        if (problemsForFile != null) {
            return Collections.unmodifiableList(problemsForFile);
        }
        return Collections.emptyList();
    }

    public Map<PsiFile, List<ProblemDescriptor>> getAllProblems() {
        return Collections.unmodifiableMap(problems);
    }

    private void addProblem(final PsiFile psiFile, final ProblemDescriptor problemDescriptor) {
        List<ProblemDescriptor> problemsForFile = problems.get(psiFile);
        if (problemsForFile == null) {
            problemsForFile = new ArrayList<ProblemDescriptor>();
            problems.put(psiFile, problemsForFile);
        }

        problemsForFile.add(problemDescriptor);
    }

    /**
     * Runnable to process an audit event.
     */
    private class ProcessResultsThread implements Runnable {

        public void run() {
            final Map<PsiFile, List<Integer>> lineLengthCachesByFile = new HashMap<PsiFile, List<Integer>>();

            synchronized (errors) {
                for (final AuditEvent event : errors) {
                    final PsiFile psiFile = fileNamesToPsiFiles.get(event.getFileName());
                    if (psiFile == null) {
                        if (LOG.isInfoEnabled()) {
                            LOG.info("Could not find mapping for file: " + event.getFileName()
                                    + " in " + fileNamesToPsiFiles);
                        }
                        return;
                    }

                    List<Integer> lineLengthCache = lineLengthCachesByFile.get(psiFile);
                    if (lineLengthCache == null) {
                        // we cache the offset of each line as it is created, so as to
                        // avoid retreating ground we've already covered.
                        lineLengthCache = new ArrayList<Integer>();
                        lineLengthCache.add(0); // line 1 is offset 0

                        lineLengthCachesByFile.put(psiFile, lineLengthCache);
                    }

                    processEvent(psiFile, lineLengthCache, event);
                }
            }
        }

        private void processEvent(final PsiFile psiFile,
                                  final List<Integer> lineLengthCache,
                                  final AuditEvent event) {
            final char[] text = psiFile.textToCharArray();

            // check for package HTML siblings, as our scan can't find these
            // if we're using a temporary file

            if (checks != null) {
                for (final Check check : checks) {
                    if (!check.process(psiFile, event)) {
                        return;
                    }
                }
            }

            int offset;
            boolean afterEndOfLine = false;

            if (event.getLine() == 0) {
                offset = event.getColumn();

            } else if (event.getLine() <= lineLengthCache.size()) {
                offset = lineLengthCache.get(event.getLine() - 1) + event.getColumn();

            } else {
                // start from end of cached data
                offset = lineLengthCache.get(lineLengthCache.size() - 1);
                int line = lineLengthCache.size();

                int column = 0;
                for (int i = offset; i < text.length; ++i) {
                    final char character = text[i];

                    // for linefeeds we need to handle CR, LF and CRLF,
                    // hence we accept either and only trigger a new
                    // line on the LF of CRLF.
                    final char nextChar = (i + 1) < text.length ? text[i + 1] : '\0';
                    if (character == '\n' || character == '\r' && nextChar != '\n') {
                        ++line;
                        ++offset;
                        lineLengthCache.add(offset);
                        column = 0;
                    } else if (character == '\t') {
                        column += tabWidth;
                        ++offset;
                    } else {
                        ++column;
                        ++offset;
                    }

                    if (event.getLine() == line && event.getColumn() == column) {
                        if (column == 0 && Character.isWhitespace(nextChar)) {
                            afterEndOfLine = true;
                        }
                        break;
                    }
                }
            }

            final PsiElement victim;
            victim = psiFile.findElementAt(offset);

            if (victim == null) {
                LOG.warn("Couldn't find victim for error: " + event.getFileName() + "("
                        + event.getLine() + ":" + event.getColumn() + ") " + event.getMessage());
            } else {
                try {
                    final ProblemDescriptor problem = manager.createProblemDescriptor(
                            victim, messageFor(event), null, problemHighlightTypeFor(event.getSeverityLevel()),
                            false, afterEndOfLine);

                    if (usingExtendedDescriptors) {
                        addProblem(psiFile, extendDescriptor(event, problem));
                    } else {
                        addProblem(psiFile, problem);
                    }

                } catch (PsiInvalidElementAccessException e) {
                    LOG.error("Element access failed", e);
                }
            }
        }

        private String messageFor(final AuditEvent event) {
            if (event.getLocalizedMessage() != null) {
                return event.getLocalizedMessage().getMessage();
            }
            return event.getMessage();
        }

        private ProblemDescriptor extendDescriptor(final AuditEvent event,
                                                   final ProblemDescriptor problem) {
            return new ExtendedProblemDescriptor(problem, event.getSeverityLevel(),
                    event.getLine(), event.getColumn());
        }


        @NotNull
        private ProblemHighlightType problemHighlightTypeFor(final SeverityLevel severityLevel) {
            if (severityLevel != null && !suppressErrors) {
                switch (severityLevel) {
                    case ERROR:
                        return ProblemHighlightType.ERROR;
                    case WARNING:
                        return ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
                    case INFO:
                        return ProblemHighlightType.WEAK_WARNING;
                    case IGNORE:
                        return ProblemHighlightType.INFORMATION;
                    default:
                        return ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
                }
            }
            return ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
        }
    }

}
