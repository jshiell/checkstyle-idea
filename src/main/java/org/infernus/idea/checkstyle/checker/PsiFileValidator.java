package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PsiFileValidator {

    private PsiFileValidator() {
    }

    public static boolean isScannable(final @Nullable PsiFile psiFile) {
        return psiFile != null
                && psiFile.isValid()
                && psiFile.isPhysical()
                && isInSource(psiFile);
    }

    private static boolean isInSource(final @NotNull PsiFile psiFile) {
        return ProjectFileIndex.SERVICE.getInstance(psiFile.getProject())
                .isInSourceContent(psiFile.getVirtualFile());
    }

}
