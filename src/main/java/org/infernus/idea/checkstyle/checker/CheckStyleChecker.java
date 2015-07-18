package org.infernus.idea.checkstyle.checker;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.psi.PsiFile;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.infernus.idea.checkstyle.CheckStyleConfiguration;
import org.infernus.idea.checkstyle.checks.CheckFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CheckStyleChecker {
    private final Checker checker;
    private final int tabWidth;

    public CheckStyleChecker(final Checker checker, final int tabWidth) {
        if (checker == null) {
            throw new IllegalArgumentException("checker may not be null");
        }

        this.checker = checker;
        this.tabWidth = tabWidth;
    }

    public Map<PsiFile, List<ProblemDescriptor>> process(final List<ScannableFile> scannableFiles,
                                                         final InspectionManager manager,
                                                         final boolean useExtendedDescriptors,
                                                         final CheckStyleConfiguration pluginConfig,
                                                         final Configuration checkerConfig) {
        return processAndAudit(filesOf(scannableFiles),
                createListener(mapFilesToElements(scannableFiles), manager, useExtendedDescriptors, pluginConfig, checkerConfig))
                .getAllProblems();
    }

    private Map<String, PsiFile> mapFilesToElements(final List<ScannableFile> scannableFiles) {
        final Map<String, PsiFile> filePathsToElements = new HashMap<>();
        for (ScannableFile scannableFile : scannableFiles) {
            filePathsToElements.put(scannableFile.getAbsolutePath(), scannableFile.getPsiFile());
        }
        return filePathsToElements;
    }

    private List<File> filesOf(final List<ScannableFile> scannableFiles) {
        return scannableFiles.stream()
                .map(ScannableFile::getFile)
                .collect(Collectors.toList());
    }

    private CheckStyleAuditListener processAndAudit(final List<File> files,
                                                    final CheckStyleAuditListener auditListener) {
        synchronized (checker) {
            checker.addListener(auditListener);
            checker.process(files);
            checker.removeListener(auditListener);
        }
        return auditListener;
    }

    private CheckStyleAuditListener createListener(final Map<String, PsiFile> filesToScan,
                                                   final InspectionManager manager,
                                                   final boolean useExtendedDescriptors,
                                                   final CheckStyleConfiguration pluginConfig,
                                                   final Configuration checkerConfig) {
        return new CheckStyleAuditListener(filesToScan, manager, useExtendedDescriptors,
                pluginConfig.isSuppressingErrors(), tabWidth, CheckFactory.getChecks(checkerConfig));
    }

    public int getTabWidth() {
        return tabWidth;
    }

    public void destroy() {
        checker.destroy();
    }
}
