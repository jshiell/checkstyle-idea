package org.infernus.idea.checkstyle.checker;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.CheckStyleConfiguration;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.jetbrains.annotations.NotNull;

public class CheckStyleChecker
{
    /** checker with config */
    private final CheckstyleInternalObject checkstyleInternalObjects;

    private final int tabWidth;
    private final Optional<String> baseDir;


    public CheckStyleChecker(@NotNull final CheckstyleInternalObject pCheckstyleInternalObjects, final int tabWidth,
                             @NotNull final Optional<String> baseDir) {
        this.checkstyleInternalObjects = pCheckstyleInternalObjects;
        this.tabWidth = tabWidth;
        this.baseDir = baseDir;
    }

    @NotNull
    public Map<PsiFile, List<Problem>> scan(@NotNull final List<ScannableFile> scannableFiles, @NotNull final
    CheckStyleConfiguration pluginConfig) {

        final CheckstyleProjectService csService = CheckstyleProjectService.getInstance(pluginConfig.getProject());
        return csService.getCheckstyleInstance().scan(checkstyleInternalObjects, scannableFiles, pluginConfig
                .isSuppressingErrors(), tabWidth, baseDir);
    }


    public void destroy(@NotNull final Project pProject) {
        final CheckstyleProjectService csService = CheckstyleProjectService.getInstance(pProject);
        csService.getCheckstyleInstance().destroyChecker(checkstyleInternalObjects);
    }


    public CheckstyleInternalObject getCheckerWithConfig4UnitTest() {
        return checkstyleInternalObjects;
    }
}
