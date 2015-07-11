package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.roots.JavaProjectRootsUtil;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PsiFileValidator {

    private PsiFileValidator() {
    }

    public static boolean isScannable(@Nullable final PsiFile psiFile) {
        return psiFile != null
                && psiFile.isValid()
                && psiFile.isPhysical()
                && isInSource(psiFile)
                && !isGenerated(psiFile);
    }

    private static boolean isGenerated(final PsiFile psiFile) {
        return JavaProjectRootsUtil.isInGeneratedCode(psiFile.getVirtualFile(), psiFile.getProject());
    }

    private static boolean isInSource(@NotNull final PsiFile psiFile) {
        return ProjectFileIndex.SERVICE.getInstance(psiFile.getProject())
                .isInSourceContent(psiFile.getVirtualFile());
    }

}
