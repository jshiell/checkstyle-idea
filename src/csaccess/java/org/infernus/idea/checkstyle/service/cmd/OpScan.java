package org.infernus.idea.checkstyle.service.cmd;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import org.infernus.idea.checkstyle.checker.Problem;
import org.infernus.idea.checkstyle.checker.ScannableFile;
import org.infernus.idea.checkstyle.checks.CheckFactory;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.infernus.idea.checkstyle.service.CheckStyleAuditListener;
import org.infernus.idea.checkstyle.service.entities.CheckerWithConfig;
import org.jetbrains.annotations.NotNull;


public class OpScan
        implements CheckstyleCommand<Map<PsiFile, List<Problem>>>
{
    private final CheckerWithConfig checkerWithConfig;

    private final List<ScannableFile> scannableFiles;

    private final boolean isSuppressingErrors;

    private final int tabWidth;

    private final Optional<String> baseDir;


    public OpScan(@NotNull final CheckstyleInternalObject pCheckerWithConfig, @NotNull final List<ScannableFile>
            pScannableFiles, final boolean pIsSuppressingErrors, final int pTabWidth, final Optional<String> pBaseDir) {
        checkerWithConfig = (CheckerWithConfig) pCheckerWithConfig;
        scannableFiles = pScannableFiles;
        isSuppressingErrors = pIsSuppressingErrors;
        tabWidth = pTabWidth;
        baseDir = pBaseDir;
    }


    @NotNull
    @Override
    public Map<PsiFile, List<Problem>> execute(@NotNull final Project pProject) throws CheckstyleException {

        if (scannableFiles.isEmpty()) {
            return Collections.emptyMap();
        }
        return processAndAudit(filesOf(scannableFiles), createListener(mapFilesToElements(scannableFiles),
                isSuppressingErrors, pProject)).getProblems();
    }


    private Map<String, PsiFile> mapFilesToElements(final List<ScannableFile> scannableFiles) {
        final Map<String, PsiFile> filePathsToElements = new HashMap<>();
        for (ScannableFile scannableFile : scannableFiles) {
            filePathsToElements.put(scannableFile.getAbsolutePath(), scannableFile.getPsiFile());
        }
        return filePathsToElements;
    }


    private List<File> filesOf(final List<ScannableFile> scannableFiles) {
        return scannableFiles.stream().map(ScannableFile::getFile).collect(Collectors.toList());
    }


    private CheckStyleAuditListener processAndAudit(final List<File> files, final CheckStyleAuditListener
            auditListener) throws CheckstyleException {
        final Checker checker = checkerWithConfig.getChecker();
        synchronized (checkerWithConfig.getChecker()) {
            checker.addListener(auditListener);
            try {
                checker.process(files);
            } finally {
                checker.removeListener(auditListener);
            }
        }
        return auditListener;
    }


    private CheckStyleAuditListener createListener(final Map<String, PsiFile> filesToScan, final boolean
            pIsSuppressingErrors, final Project pProject) {
        return new CheckStyleAuditListener(filesToScan, pIsSuppressingErrors, tabWidth, baseDir, CheckFactory
                .getChecks(pProject, checkerWithConfig));
    }
}
