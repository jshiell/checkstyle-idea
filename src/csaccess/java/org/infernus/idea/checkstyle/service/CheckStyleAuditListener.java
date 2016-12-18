package org.infernus.idea.checkstyle.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.checker.Problem;
import org.infernus.idea.checkstyle.checks.Check;
import org.infernus.idea.checkstyle.csapi.SeverityLevel;
import org.jetbrains.annotations.NotNull;
import java.util.Set;

public class CheckStyleAuditListener
        implements AuditListener
{
    private static final Log LOG = LogFactory.getLog(CheckStyleAuditListener.class);

    private final boolean suppressErrors;
    private final List<Check> checks;
    private final int tabWidth;
    private final Optional<String> baseDir;
    private final Map<String, PsiFile> fileNamesToPsiFiles;

    private final List<AuditEvent> errors = new ArrayList<>();
    private final Map<PsiFile, List<Problem>> problems = new HashMap<>();

    public CheckStyleAuditListener(@NotNull final Map<String, PsiFile> fileNamesToPsiFiles, final boolean
            suppressErrors, final int tabWidth, @NotNull final Optional<String> baseDir, @NotNull final List<Check>
            checks) {
        this.fileNamesToPsiFiles = new HashMap<>(fileNamesToPsiFiles);
        this.checks = checks;
        this.suppressErrors = suppressErrors;
        this.tabWidth = tabWidth;
        this.baseDir = baseDir;
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

    public void addException(final AuditEvent auditEvent, final Throwable throwable) {
        LOG.error("Exception during CheckStyle execution", throwable);
        synchronized (errors) {
            errors.add(auditEvent);
        }
    }

    @NotNull
    public Map<PsiFile, List<Problem>> getProblems() {
        return Collections.unmodifiableMap(problems);
    }

    private void addProblem(final PsiFile psiFile, final Problem problem) {
        List<Problem> problemsForFile = problems.get(psiFile);
        if (problemsForFile == null) {
            problemsForFile = new ArrayList<>();
            problems.put(psiFile, problemsForFile);
        }

        problemsForFile.add(problem);
    }

    private class ProcessResultsThread
            implements Runnable
    {

        @Override
        public void run() {
            final Map<PsiFile, List<Integer>> lineLengthCachesByFile = new HashMap<>();

            synchronized (errors) {
                for (final AuditEvent event : errors) {
                    final PsiFile psiFile = fileNamesToPsiFiles.get(filenameFrom(event));
                    if (psiFile == null) {
                        if (LOG.isInfoEnabled()) {
                            LOG.info("Could not find mapping for file: " + event.getFileName() + " in " +
                                    fileNamesToPsiFiles);
                        }
                        return;
                    }

                    List<Integer> lineLengthCache = lineLengthCachesByFile.get(psiFile);
                    if (lineLengthCache == null) {
                        // we cache the offset of each line as it is created, so as to
                        // avoid retreating ground we've already covered.
                        lineLengthCache = new ArrayList<>();
                        lineLengthCache.add(0); // line 1 is offset 0

                        lineLengthCachesByFile.put(psiFile, lineLengthCache);
                    }

                    processEvent(psiFile, lineLengthCache, event);
                }
            }
        }

        private String filenameFrom(final AuditEvent event) {
            return baseDir.map(prefix -> withTrailingSeparator(prefix) + event.getFileName()).orElseGet
                    (event::getFileName);
        }

        private String withTrailingSeparator(final String path) {
            if (path != null && !path.endsWith(File.separator)) {
                return path + File.separator;
            }
            return path;
        }

        private void processEvent(final PsiFile psiFile, final List<Integer> lineLengthCache, final AuditEvent event) {
            if (additionalChecksFail(psiFile, event)) {
                return;
            }

            final Position position = findPosition(lineLengthCache, event, psiFile.textToCharArray());
            final PsiElement victim = position.element(psiFile);

            if (victim != null) {
                addProblemTo(victim, psiFile, event, position.afterEndOfLine);
            } else {
                addProblemTo(psiFile, psiFile, event, false);
                LOG.debug("Couldn't find victim for error: " + event.getFileName() + "(" + event.getLine() + ":" +
                        event.getColumn() + ") " + event.getMessage());
            }
        }

        private void addProblemTo(final PsiElement victim, final PsiFile psiFile, @NotNull final AuditEvent event,
                                  final boolean afterEndOfLine) {
            String message = event.getMessage();
            if (event.getLocalizedMessage() != null) {
                message = event.getLocalizedMessage().getMessage();
            }
            final SeverityLevel severityLevel = readSeverityLevel(event.getSeverityLevel());
            try {
                addProblem(psiFile, new Problem(victim, message, severityLevel, event.getLine(), event.getColumn(),
                        afterEndOfLine, suppressErrors));

            } catch (PsiInvalidElementAccessException e) {
                LOG.error("Element access failed", e);
            }
        }

        private SeverityLevel readSeverityLevel(final com.puppycrawl.tools.checkstyle.api.SeverityLevel
                                                        pSeverityLevel) {
            SeverityLevel result = null;
            if (pSeverityLevel != null) {
                switch (pSeverityLevel) {
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

        private boolean additionalChecksFail(final PsiFile psiFile, final AuditEvent event) {
            for (final Check check : checks) {
                if (!check.process(psiFile, event.getSourceName())) {
                    return true;
                }
            }
            return false;
        }

        @NotNull
        private Position findPosition(final List<Integer> lineLengthCache, final AuditEvent event, final char[] text) {
            if (event.getLine() == 0) {
                return Position.at(event.getColumn());

            } else if (event.getLine() <= lineLengthCache.size()) {
                return Position.at(lineLengthCache.get(event.getLine() - 1) + event.getColumn());

            } else {
                return searchFromEndOfCachedData(lineLengthCache, event, text);
            }
        }

        @NotNull
        private Position searchFromEndOfCachedData(final List<Integer> lineLengthCache, final AuditEvent event, final
        char[] text) {
            final Position position;
            int offset = lineLengthCache.get(lineLengthCache.size() - 1);
            boolean afterEndOfLine = false;
            int line = lineLengthCache.size();

            int column = 0;
            for (int i = offset; i < text.length; ++i) {
                final char character = text[i];

                // for linefeeds we need to handle CR, LF and CRLF,
                // hence we accept either and only trigger a new
                // line on the LF of CRLF.
                final char nextChar = nextCharacter(text, i);
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

            position = Position.at(offset, afterEndOfLine);
            return position;
        }

        private char nextCharacter(final char[] text, final int i) {
            if ((i + 1) < text.length) {
                return text[i + 1];
            }
            return '\0';
        }

    }

    private static final class Position
    {
        private final boolean afterEndOfLine;
        private final int offset;

        public static Position at(final int offset, final boolean afterEndOfLine) {
            return new Position(offset, afterEndOfLine);
        }

        public static Position at(final int offset) {
            return new Position(offset, false);
        }

        private Position(final int offset, final boolean afterEndOfLine) {
            this.offset = offset;
            this.afterEndOfLine = afterEndOfLine;
        }

        private PsiElement element(final PsiFile psiFile) {
            return psiFile.findElementAt(offset);
        }
    }

}
