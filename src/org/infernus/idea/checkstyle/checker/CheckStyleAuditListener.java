package org.infernus.idea.checkstyle.checker;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.util.ExtendedProblemDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Listener for the CheckStyle process.
 *
 * @author James Shiell
 * @version 1.1
 */
public class CheckStyleAuditListener implements AuditListener {

    private static final Log LOG = LogFactory.getLog(
            CheckStyleAuditListener.class);

    private final boolean usingExtendedDescriptors;

    private final PsiFile psiFile;
    private final InspectionManager manager;

    private List<AuditEvent> errors = new ArrayList<AuditEvent>();
    private List<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();

    /**
     * Create a new listener.
     *
     * @param psiFile the file being checked.
     * @param manager the current inspection manager.
     */
    public CheckStyleAuditListener(final PsiFile psiFile,
                                   final InspectionManager manager) {
        this(psiFile, manager, false);
    }

    /**
     * Create a new listener.
     * <p/>
     * Use the second argument to determine if we return our extended problem
     * descriptors. This is provided to avoid problems with downstream code
     * that may be interested in the implementation type.
     *
     * @param psiFile                the file being checked.
     * @param manager                the current inspection manager.
     * @param useExtendedDescriptors should we return standard IntelliJ
     *                               problem descriptors or extended ones with severity information?
     */
    public CheckStyleAuditListener(final PsiFile psiFile,
                                   final InspectionManager manager,
                                   final boolean useExtendedDescriptors) {
        this.psiFile = psiFile;
        this.manager = manager;
        this.usingExtendedDescriptors = useExtendedDescriptors;
    }

    /**
     * {@inheritDoc}
     */
    public void auditStarted(final AuditEvent auditEvent) {
        errors.clear();
        problems.clear();
    }

    /**
     * {@inheritDoc}
     */
    public void auditFinished(final AuditEvent auditEvent) {
        final ProcessResultsThread findThread = new ProcessResultsThread();
        
        final Application application = ApplicationManager.getApplication();
        if (application.isDispatchThread()) {
            findThread.run();
        } else {
            application.runReadAction(findThread);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void fileStarted(final AuditEvent auditEvent) {

    }

    /**
     * {@inheritDoc}
     */
    public void fileFinished(final AuditEvent auditEvent) {

    }

    /**
     * {@inheritDoc}
     */
    public void addError(final AuditEvent auditEvent) {
        errors.add(auditEvent);
    }

    /**
     * {@inheritDoc}
     */
    public void addException(final AuditEvent auditEvent,
                             final Throwable throwable) {
        LOG.error("Exception during CheckStyle execution", throwable);
        errors.add(auditEvent);
    }

    /**
     * Get the problems found by this scan.
     *
     * @return the problems found by this scan.
     */
    public List<ProblemDescriptor> getProblems() {
        return problems;
    }

    /**
     * Runnable to process an audit event.
     */
    private class ProcessResultsThread implements Runnable {
        private static final String PACKAGE_HTML_CHECK
                = "com.puppycrawl.tools.checkstyle.checks.javadoc.PackageHtmlCheck";
        private static final String PACKAGE_HTML_FILE = "package.html";

        /**
         * {@inheritDoc}
         */
        public void run() {
            final char[] text = psiFile.textToCharArray();

            // we cache the offset of each line as it is created, so as to
            // avoid retreating ground we've already covered.
            final List<Integer> lineLengthCache = new ArrayList<Integer>();
            lineLengthCache.add(0); // line 1 is offset 0

            AuditLoop:
            for (final AuditEvent event : errors) {
                // check for package HTML siblings, as our scan can't find these
                // if we're using a temporary file

                if (PACKAGE_HTML_CHECK.equals(event.getSourceName())) {
                    // find the first sibling
                    PsiElement currentSibling = psiFile;
                    while (currentSibling.getPrevSibling() != null) {
                        currentSibling = currentSibling.getPrevSibling();
                    }

                    while (currentSibling != null) {
                        if (currentSibling.isPhysical() && currentSibling.isValid()
                                && currentSibling instanceof PsiFile
                                && PACKAGE_HTML_FILE.equals(((PsiFile) currentSibling).getName())) {
                            continue AuditLoop;
                        }

                        currentSibling = currentSibling.getNextSibling();
                    }
                }

                int offset;
                boolean endOfLine = false;

                // start of file
                if (event.getLine() == 0) { // start of file errors
                    offset = event.getColumn();

                    // line offset is cached...
                } else if (event.getLine() <= lineLengthCache.size()) {
                    offset = lineLengthCache.get(event.getLine() - 1) + event.getColumn();

                    // further search required
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
                        } else {
                            ++column;
                            ++offset;
                        }

                        // need to go to end of line though
                        if (event.getLine() == line && event.getColumn() == column) {
                            if (column == 0 && Character.isWhitespace(nextChar)) {
                                // move line errors to after EOL
                                endOfLine = true;
                            }
                            break;
                        }
                    }
                }


                final PsiElement victim;
                victim = psiFile.findElementAt(offset);

                if (victim == null) {
                    LOG.error("Couldn't find victim for error: " + event.getFileName() + "("
                            + event.getLine() + ":" + event.getColumn() + ") " + event.getMessage());
                } else {
                    final String message = event.getLocalizedMessage() != null
                            ? event.getLocalizedMessage().getMessage()
                            : event.getMessage();
                    final ProblemHighlightType problemType
                            = ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
                    final ProblemDescriptor problem = manager.createProblemDescriptor(
                            victim, message, null, problemType, endOfLine);

                    if (usingExtendedDescriptors) {
                        final ProblemDescriptor delegate
                                = new ExtendedProblemDescriptor(
                                problem, event.getSeverityLevel(),
                                event.getLine(), event.getColumn());
                        problems.add(delegate);
                    } else {
                        problems.add(problem);
                    }
                }
            }
        }
    }

}
