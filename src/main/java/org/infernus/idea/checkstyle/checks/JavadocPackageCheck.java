package org.infernus.idea.checkstyle.checks;

import java.util.concurrent.atomic.AtomicReference;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.infernus.idea.checkstyle.csapi.ConfigVisitor;
import org.infernus.idea.checkstyle.csapi.ConfigurationModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Extra logic for the JavadocPackageCheck check.
 */
public final class JavadocPackageCheck implements Check {

    private static final String CHECK_PACKAGE_INFO =
            "com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocPackageCheck";
    private static final String MODULE_NAME = "JavadocPackage";
    private static final String PACKAGE_HTML_FILE = "package.html";
    private static final String PACKAGE_INFO_FILE = "package-info.java";

    private boolean usingLegacyPackage;

    private final Project project;

    public JavadocPackageCheck(@NotNull final Project pProject) {
        project = pProject;
    }

    @Override
    public String getShortName() {
        return MODULE_NAME;
    }

    @Override
    public String getFullyQualifiedName() {
        return CHECK_PACKAGE_INFO;
    }

    /**
     * Retrieve the allowLegacy flag for handling javadoc package info from a CheckStyle configuration.
     */
    public void configure(@NotNull final CheckstyleInternalObject config) {
        final String stringValue = parsePackageInfoLegacy(config);
        usingLegacyPackage = null != stringValue && Boolean.parseBoolean(stringValue);
    }

    public boolean process(@NotNull final PsiFile file, @NotNull final String pEventSourceName) {
        if (!CHECK_PACKAGE_INFO.equals(pEventSourceName)) {
            return true;
        }

        PsiElement currentSibling = findFirstSibling(file);

        while (currentSibling != null) {
            if (currentSibling.isPhysical() && currentSibling.isValid() && currentSibling instanceof PsiFile) {
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
    @Nullable
    private String parsePackageInfoLegacy(final CheckstyleInternalObject config) {
        if (config == null) {
            return null;
        }

        // TODO Going through the whole config is somewhat inefficient here; might do this centrally and only
        // once. Currently we don't care because there is only one such class.
        final CheckstyleProjectService csService = CheckstyleProjectService.getInstance(project);
        final AtomicReference<String> value = new AtomicReference<>();
        csService.getCheckstyleInstance().peruseConfiguration(config, new ConfigVisitor() {
            @Override
            public void visit(@NotNull final ConfigurationModule pModule) {
                if (MODULE_NAME.equals(pModule.getName()) || CHECK_PACKAGE_INFO.equals(pModule.getName())) {
                    value.set(pModule.getProperties().get("allowLegacy"));
                    // TODO This means that if for some reasons this attribute appears multiple times, the last
                    // occurrence wins. Instead, we should check if 'allowLegacy' is true anywhere.
                }
            }
        });
        return value.get();
    }
}
