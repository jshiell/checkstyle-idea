package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.csapi.CheckstyleActions;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.infernus.idea.checkstyle.util.ClassLoaderDumper;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;


public class CheckStyleChecker {

    private static final Logger LOG = Logger.getInstance(CheckStyleChecker.class);

    private final CheckstyleInternalObject checkerWithConfig;
    private final CheckstyleActions csServiceInstance;

    private final int tabWidth;
    private final Optional<String> baseDir;

    public CheckStyleChecker(@NotNull final CheckstyleInternalObject checkerWithConfig,
                             final int tabWidth,
                             @NotNull final Optional<String> baseDir,
                             @NotNull final CheckstyleActions csServiceInstance) {
        this.checkerWithConfig = checkerWithConfig;
        this.tabWidth = tabWidth;
        this.baseDir = baseDir;
        this.csServiceInstance = csServiceInstance;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating Checkstyle instances with CheckStyle classpath:\n"
                    + ClassLoaderDumper.dumpClassLoader(csServiceInstance.getClass().getClassLoader()));
        }
    }

    @NotNull
    public Map<PsiFile, List<Problem>> scan(@NotNull final List<ScannableFile> scannableFiles,
                                            final boolean suppressErrors) {
        return csServiceInstance.scan(checkerWithConfig, scannableFiles, suppressErrors, tabWidth, baseDir);
    }

    public void destroy() {
        csServiceInstance.destroyChecker(checkerWithConfig);
    }


    public CheckstyleInternalObject getCheckerWithConfig4UnitTest() {
        return checkerWithConfig;
    }
}
