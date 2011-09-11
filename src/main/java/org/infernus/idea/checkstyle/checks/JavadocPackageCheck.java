package org.infernus.idea.checkstyle.checks;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.jetbrains.annotations.NotNull;

/**
 * Extra logic for the JavadocPackageCheck check.
 */
public final class JavadocPackageCheck implements Check {

    private static final String CHECK_PACKAGE_INFO = "com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocPackageCheck";
    private static final String MODULE_NAME = "JavadocPackage";
    private static final String PACKAGE_HTML_FILE = "package.html";
    private static final String PACKAGE_INFO_FILE = "package-info.java";

    private boolean usingLegacyPackage;

    /**
     * Retrieve the allowLegacy flag for handling javadoc package info from a CheckStyle configuration.
     */
    public void configure(@NotNull final Configuration config) {
        final String stringValue = parsePackageInfoLegacy(config);
        usingLegacyPackage = null != stringValue && Boolean.parseBoolean(stringValue);
    }

    public boolean process(@NotNull final PsiFile file, @NotNull final AuditEvent event) {
        if (!CHECK_PACKAGE_INFO.equals(event.getSourceName())) {
            return true;
        }

        PsiElement currentSibling = findFirstSibling(file);

        while (currentSibling != null) {
            if (currentSibling.isPhysical() && currentSibling.isValid()
                    && currentSibling instanceof PsiFile) {
                final String siblingName = ((PsiFile) currentSibling).getName();
                if (PACKAGE_INFO_FILE.equals(siblingName)
                        || (usingLegacyPackage && PACKAGE_HTML_FILE.equals(siblingName))) {
                    return false;
                }
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

    /**
     * Retrieve the allowLegacy flag as a String for handling javadoc package info from a CheckStyle configuration.
     *
     * @param config the checkstyle configuration.
     * @return the allowLegacy flag in String form.
     */
    private String parsePackageInfoLegacy(final Configuration config) {
        if (config == null || config.getName() == null) {
            return null;
        }

        String value;
        try {
            if (MODULE_NAME.equals(config.getName()) || CHECK_PACKAGE_INFO.equals(config.getName())) {
                value = config.getAttribute("allowLegacy");

            } else {
                Configuration[] children = config.getChildren();
                int index = 0;
                value = null;
                while (index < children.length && null == value) {
                    value = parsePackageInfoLegacy(children[index]);
                    index++;
                }
            }
        } catch (CheckstyleException ce) {
            // bummer, don't have it... return null
            value = null;
        }

        return value;
    }
}
