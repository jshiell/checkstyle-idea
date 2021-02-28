package org.infernus.idea.checkstyle.service.cmd;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import org.infernus.idea.checkstyle.checker.Problem;
import org.infernus.idea.checkstyle.checker.ScannableFile;
import org.infernus.idea.checkstyle.checks.CheckFactory;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.infernus.idea.checkstyle.exception.CheckstyleVersionMixException;
import org.infernus.idea.checkstyle.service.CheckStyleAuditListener;
import org.infernus.idea.checkstyle.service.entities.CheckerWithConfig;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

import static java.util.stream.Collectors.toList;

public class OpScan implements CheckstyleCommand<Map<PsiFile, List<Problem>>> {

    private final CheckerWithConfig checkerWithConfig;
    private final List<ScannableFile> scannableFiles;
    private final boolean suppressErrors;
    private final int tabWidth;
    private final Optional<String> baseDir;

    public OpScan(@NotNull final CheckstyleInternalObject checkerWithConfig,
                  @NotNull final List<ScannableFile> scannableFiles,
                  final boolean suppressErrors,
                  final int tabWidth,
                  final Optional<String> baseDir) {
        if (!(checkerWithConfig instanceof CheckerWithConfig)) {
            throw new CheckstyleVersionMixException(CheckerWithConfig.class, checkerWithConfig);
        }
        this.checkerWithConfig = (CheckerWithConfig) checkerWithConfig;
        this.scannableFiles = scannableFiles;
        this.suppressErrors = suppressErrors;
        this.tabWidth = tabWidth;
        this.baseDir = baseDir;
    }

    @NotNull
    @Override
    public Map<PsiFile, List<Problem>> execute(@NotNull final Project project) throws CheckstyleException {
        if (scannableFiles.isEmpty()) {
            return Collections.emptyMap();
        }
        return processAndAudit(filesOf(scannableFiles), createListener(mapFilesToElements(scannableFiles), project))
                .getProblems();
    }

    private Map<String, PsiFile> mapFilesToElements(final List<ScannableFile> filesToScan) {
        final Map<String, PsiFile> filePathsToElements = new HashMap<>();
        for (ScannableFile scannableFile : filesToScan) {
            filePathsToElements.put(scannableFile.getAbsolutePath(), scannableFile.getPsiFile());
        }
        return filePathsToElements;
    }

    private List<File> filesOf(final List<ScannableFile> filesToScan) {
        return filesToScan.stream().map(ScannableFile::getFile).collect(toList());
    }

    private CheckStyleAuditListener processAndAudit(final List<File> files,
                                                    final CheckStyleAuditListener auditListener)
            throws CheckstyleException {
        final Checker checker = checkerWithConfig.getChecker();
        checkerWithConfig.getCheckerLock().lock();
        checker.addListener(auditListener);
        try {
            checker.process(files);
        } finally {
            checker.removeListener(auditListener);
            checkerWithConfig.getCheckerLock().unlock();
        }
        return auditListener;
    }

    private CheckStyleAuditListener createListener(final Map<String, PsiFile> filesToScan,
                                                   final Project project) {
        return new CheckStyleAuditListener(filesToScan, suppressErrors, tabWidth, baseDir,
                CheckFactory.getChecks(project, checkerWithConfig));
    }
}
