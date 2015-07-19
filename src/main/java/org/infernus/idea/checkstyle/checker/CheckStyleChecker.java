package org.infernus.idea.checkstyle.checker;

import com.intellij.psi.PsiFile;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.infernus.idea.checkstyle.CheckStyleConfiguration;
import org.infernus.idea.checkstyle.checks.CheckFactory;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;

public class CheckStyleChecker {
    private final Checker checker;
    private final Configuration configuration;
    private final int tabWidth;

    public CheckStyleChecker(@NotNull final Checker checker,
                             @NotNull final Configuration configuration,
                             final int tabWidth) {
        this.checker = checker;
        this.configuration = configuration;
        this.tabWidth = tabWidth;
    }

    @NotNull
    public Map<PsiFile, List<Problem>> scan(@NotNull final List<ScannableFile> scannableFiles,
                                            @NotNull final CheckStyleConfiguration pluginConfig) {
        if (scannableFiles.isEmpty()) {
            return emptyMap();
        }

        return processAndAudit(filesOf(scannableFiles),
                createListener(mapFilesToElements(scannableFiles), pluginConfig))
                .getProblems();
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
                                                   final CheckStyleConfiguration pluginConfig) {
        return new CheckStyleAuditListener(filesToScan,
                pluginConfig.isSuppressingErrors(), tabWidth, CheckFactory.getChecks(configuration));
    }

    public void destroy() {
        checker.destroy();
    }
}
