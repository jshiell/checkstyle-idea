package org.infernus.idea.checkstyle.csapi;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import org.infernus.idea.checkstyle.checker.Problem;
import org.infernus.idea.checkstyle.checks.Check;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.*;


public class ProcessResultsThread implements Runnable {

    private static final Logger LOG = Logger.getInstance(ProcessResultsThread.class);

    private final boolean suppressErrors;
    private final List<Check> checks;
    private final int tabWidth;
    private final Optional<String> baseDir;
    private final List<Issue> errors;
    private final Map<String, PsiFile> fileNamesToPsiFiles;

    private final Map<PsiFile, List<Problem>> problems = new HashMap<>();


    private static final class Position {
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


    public ProcessResultsThread(final boolean suppressErrors,
                                final List<Check> checks,
                                final int tabWidth,
                                final Optional<String> baseDir,
                                final List<Issue> errors,
                                final Map<String, PsiFile> fileNamesToPsiFiles) {
        this.suppressErrors = suppressErrors;
        this.checks = checks;
        this.tabWidth = tabWidth;
        this.baseDir = baseDir;
        this.errors = errors;
        this.fileNamesToPsiFiles = fileNamesToPsiFiles;
    }


    @Override
    public void run() {
        final Map<PsiFile, List<Integer>> lineLengthCachesByFile = new HashMap<>();

        for (final Issue event : errors) {
            final PsiFile psiFile = fileNamesToPsiFiles.get(filenameFrom(event));
            if (psiFile == null) {
                LOG.info("Could not find mapping for file: " + event.fileName + " in " + fileNamesToPsiFiles);
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

    private String filenameFrom(final Issue event) {
        return baseDir
                .map(prefix -> withTrailingSeparator(prefix) + event.fileName)
                .map(this::normalisePath)
                .filter(normalisedFileName -> new File(normalisedFileName).exists())
                .orElseGet(() -> normalisePath(event.fileName));
    }

    private String normalisePath(final String prefixedFileName) {
        try {
            return Paths.get(prefixedFileName).normalize().toString();
        } catch (InvalidPathException e) {
            return prefixedFileName;  // cannot normalize
        }
    }

    private String withTrailingSeparator(final String path) {
        if (path != null && !path.endsWith(File.separator)) {
            return path + File.separator;
        }
        return path;
    }

    private void processEvent(final PsiFile psiFile, final List<Integer> lineLengthCache, final Issue event) {
        if (additionalChecksFail(psiFile, event)) {
            return;
        }

        final Position position = findPosition(lineLengthCache, event, psiFile.textToCharArray());
        final PsiElement victim = position.element(psiFile);

        if (victim != null) {
            addProblemTo(victim, psiFile, event, position.afterEndOfLine);
        } else {
            addProblemTo(psiFile, psiFile, event, false);
            LOG.debug("Couldn't find victim for error: " + event.fileName + "(" + event.lineNumber + ":"
                    + event.columnNumber + ") " + event.message);
        }
    }

    private void addProblemTo(final PsiElement victim,
                              final PsiFile psiFile,
                              @NotNull final Issue event,
                              final boolean afterEndOfLine) {
        try {
            addProblem(psiFile, new Problem(victim, event.message, event.severityLevel, event.lineNumber,
                    event.columnNumber, event.sourceName, afterEndOfLine, suppressErrors));
        } catch (PsiInvalidElementAccessException e) {
            LOG.warn("Element access failed", e);
        }
    }

    private boolean additionalChecksFail(final PsiFile psiFile, final Issue event) {
        for (final Check check : checks) {
            if (!check.process(psiFile, event.sourceName)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private Position findPosition(final List<Integer> lineLengthCache, final Issue event, final char[] text) {
        if (event.lineNumber == 0) {
            return Position.at(event.columnNumber);
        } else if (event.lineNumber <= lineLengthCache.size()) {
            return Position.at(lineLengthCache.get(event.lineNumber - 1) + event.columnNumber);
        } else {
            return searchFromEndOfCachedData(lineLengthCache, event, text);
        }
    }

    @NotNull
    private Position searchFromEndOfCachedData(final List<Integer> lineLengthCache,
                                               final Issue event,
                                               final char[] text) {
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

            if (event.lineNumber == line && event.columnNumber == column) {
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


    @NotNull
    public Map<PsiFile, List<Problem>> getProblems() {
        return Collections.unmodifiableMap(problems);
    }

    private void addProblem(final PsiFile psiFile, final Problem problem) {
        problems.computeIfAbsent(psiFile, key -> new ArrayList<>()).add(problem);
    }
}
