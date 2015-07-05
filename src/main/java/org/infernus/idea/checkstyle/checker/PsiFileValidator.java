package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.util.Paths;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;

public final class PsiFileValidator {

    private PsiFileValidator() {
    }

    public static boolean isScannable(@Nullable final PsiFile psiFile) {
        return psiFile != null
                && psiFile.isValid()
                && psiFile.isPhysical()
                && isInSource(psiFile)
                && isNotInOutputPaths(psiFile);
    }

    private static boolean isNotInOutputPaths(@NotNull final PsiFile psiFile) {
        final String fileUrl = urlOf(psiFile);
        if (fileUrl == null) {
            return true;
        }

        for (Module module : ModuleManager.getInstance(psiFile.getProject()).getModules()) {
            for (URL outputPath : Paths.compilerOutputPathsFor(module)) {
                if (fileUrl.startsWith(outputPath.toString())) {
                    return false;
                }
            }
        }

        return true;
    }

    private static String urlOf(@NotNull final PsiFile psiFile) {
        if (psiFile.getVirtualFile() != null) {
            try {
                // getUrl() formats a file root as file:///, URL formats as file:/
                return new URL(psiFile.getVirtualFile().getUrl()).toString();
            } catch (MalformedURLException ignored) {
            }
        }

        return null;
    }

    private static boolean isInSource(@NotNull final PsiFile psiFile) {
        return ProjectFileIndex.SERVICE.getInstance(psiFile.getProject())
                .isInSourceContent(psiFile.getVirtualFile());
    }

}
