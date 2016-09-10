package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.roots.JavaProjectRootsUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.CheckStyleConfiguration;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.infernus.idea.checkstyle.util.FileTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

final class PsiFileValidator {

    private PsiFileValidator() {
    }

    public static boolean isScannable(@Nullable final PsiFile psiFile,
                                      @NotNull final Optional<Module> module,
                                      @NotNull final CheckStyleConfiguration pluginConfig) {
        return psiFile != null
                && psiFile.isValid()
                && psiFile.isPhysical()
                && hasDocument(psiFile)
                && isInSource(psiFile, pluginConfig)
                && isValidFileType(psiFile, pluginConfig)
                && isScannableIfTest(psiFile, pluginConfig)
                && modulesMatch(psiFile, module)
                && !isGenerated(psiFile);
    }

    private static boolean hasDocument(final PsiFile psiFile) {
        return PsiDocumentManager.getInstance(psiFile.getProject()).getDocument(psiFile) != null;
    }

    private static boolean isValidFileType(final PsiFile psiFile,
                                           final CheckStyleConfiguration pluginConfig) {
        return pluginConfig.getScanScope().includeNonJavaSources()
                || FileTypes.isJava(psiFile.getFileType());
    }

    private static boolean isScannableIfTest(final PsiFile psiFile,
                                             final CheckStyleConfiguration pluginConfig) {
        return pluginConfig.getScanScope().includeTestClasses()
                || !isTestClass(psiFile);
    }

    private static boolean isGenerated(final PsiFile psiFile) {
        return JavaProjectRootsUtil.isInGeneratedCode(psiFile.getVirtualFile(), psiFile.getProject());
    }

    private static boolean isInSource(@NotNull final PsiFile psiFile, final CheckStyleConfiguration pluginConfig) {
        return pluginConfig.getScanScope() == ScanScope.Everything
            || ProjectFileIndex.SERVICE.getInstance(psiFile.getProject()).isInSourceContent(psiFile.getVirtualFile());
    }

    private static boolean isTestClass(final PsiElement element) {
        final VirtualFile elementFile = element.getContainingFile().getVirtualFile();
        if (elementFile == null) {
            return false;
        }

        final Module module = ModuleUtil.findModuleForPsiElement(element);
        if (module == null) {
            return false;
        }

        final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
        return moduleRootManager != null
                && moduleRootManager.getFileIndex().isInTestSourceContent(elementFile);
    }

    private static boolean modulesMatch(final PsiFile psiFile,
                                        final Optional<Module> module) {
        if (!module.isPresent()) {
            return true;
        }
        final Module elementModule = ModuleUtil.findModuleForPsiElement(psiFile);
        return elementModule != null && elementModule.equals(module.get());
    }

}
