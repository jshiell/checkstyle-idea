package org.infernus.idea.checkstyle.checks;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.jetbrains.annotations.NotNull;

/**
 * Extra logic for the PackageHtmlCheck check.
 */
public class PackageHtmlCheck implements Check {

    private static final String CHECK_PACKAGE_HTML
            = "com.puppycrawl.tools.checkstyle.checks.javadoc.PackageHtmlCheck";
    private static final String PACKAGE_HTML_FILE = "package.html";

    public void configure(@NotNull final Configuration config) {
    }

    public boolean process(@NotNull final PsiFile file, @NotNull final AuditEvent event) {
        if (!CHECK_PACKAGE_HTML.equals(event.getSourceName())) {
            return true;
        }

        PsiElement currentSibling = findFirstSibling(file);

        while (currentSibling != null) {
            if (currentSibling.isPhysical() && currentSibling.isValid()
                    && currentSibling instanceof PsiFile
                    && PACKAGE_HTML_FILE.equals(((PsiFile) currentSibling).getName())) {
                return false;
            }

            currentSibling = currentSibling.getNextSibling();
        }

        return true;
    }

    private PsiElement findFirstSibling(@NotNull final PsiFile psiFile) {
        PsiElement currentSibling = psiFile;
        while (currentSibling != null && currentSibling.getPrevSibling() != null) {
            currentSibling = currentSibling.getPrevSibling();
        }
        return currentSibling;
    }
}
