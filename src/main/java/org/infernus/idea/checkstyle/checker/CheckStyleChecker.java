package org.infernus.idea.checkstyle.checker;

import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.csapi.CheckstyleActions;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;


public class CheckStyleChecker {
    /**
     * checker with config
     */
    private final CheckstyleInternalObject checkerWithConfig;

    /**
     * the service instance that was current when this object is created
     */
    private final CheckstyleActions csServiceInstance;

    private final int tabWidth;
    private final Optional<String> baseDir;

    private final ClassLoader loaderOfCheckedCode;


    public CheckStyleChecker(@NotNull final CheckstyleInternalObject checkerWithConfig,
                             final int tabWidth,
                             @NotNull final Optional<String> baseDir,
                             final ClassLoader loaderOfCheckedCode,
                             @NotNull final CheckstyleActions csServiceInstance) {
        this.csServiceInstance = csServiceInstance;
        this.checkerWithConfig = checkerWithConfig;
        this.tabWidth = tabWidth;
        this.baseDir = baseDir;
        this.loaderOfCheckedCode = loaderOfCheckedCode;
    }

    @NotNull
    public Map<PsiFile, List<Problem>> scan(@NotNull final List<ScannableFile> scannableFiles,
                                            final boolean suppressErrors) {
        return csServiceInstance.scan(checkerWithConfig, scannableFiles, suppressErrors, tabWidth, baseDir);
    }


    public void destroy() {
        csServiceInstance.destroyChecker(checkerWithConfig);
    }


    public ClassLoader getLoaderOfCheckedCode() {
        return loaderOfCheckedCode;
    }


    public CheckstyleInternalObject getCheckerWithConfig4UnitTest() {
        return checkerWithConfig;
    }
}
