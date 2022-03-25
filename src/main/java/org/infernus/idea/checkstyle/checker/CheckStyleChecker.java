package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import org.infernus.idea.checkstyle.csapi.CheckstyleActions;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;
import org.infernus.idea.checkstyle.util.ClassLoaderDumper;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


public class CheckStyleChecker {

    private static final Logger LOG = Logger.getInstance(CheckStyleChecker.class);

    private final CheckstyleInternalObject checkerWithConfig;
    private final CheckstyleActions csServiceInstance;

    private final int tabWidth;
    private final Optional<String> baseDir;
    private final Optional<NamedScope> namedScope;

    public CheckStyleChecker(@NotNull final CheckstyleInternalObject checkerWithConfig,
                             final int tabWidth,
                             @NotNull final Optional<String> baseDir,
                             @NotNull final CheckstyleActions csServiceInstance,
                             final Optional<NamedScope> namedScope) {
        this.checkerWithConfig = checkerWithConfig;
        this.tabWidth = tabWidth;
        this.baseDir = baseDir;
        this.csServiceInstance = csServiceInstance;
        this.namedScope = namedScope;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating Checkstyle instances with CheckStyle classpath:\n"
                    + ClassLoaderDumper.dumpClassLoader(csServiceInstance.getClass().getClassLoader()));
        }
    }

    @NotNull
    public Map<PsiFile, List<Problem>> scan(@NotNull final List<ScannableFile> scannableFiles,
                                            final boolean suppressErrors) {
        final List<ScannableFile> filteredFiles = this.namedScope.map(scope -> scannableFiles.stream()
                        .filter(scannableFile -> NamedScopeHelper.isFileInScope(scannableFile.getPsiFile(), scope))
                        .collect(Collectors.toList()))
                .orElse(scannableFiles);

        if (filteredFiles.isEmpty()) {
            return Collections.emptyMap();
        }

        return csServiceInstance.scan(checkerWithConfig, filteredFiles, suppressErrors, tabWidth, baseDir);
    }

    public void destroy() {
        csServiceInstance.destroyChecker(checkerWithConfig);
    }


    public CheckstyleInternalObject getCheckerWithConfig4UnitTest() {
        return checkerWithConfig;
    }
}
