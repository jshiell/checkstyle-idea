package org.infernus.idea.checkstyle.checks;

import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.jetbrains.annotations.NotNull;

/**
 * Allows extra logic for a certain Check.
 * <p/>
 * Not a lot of extra logic at present, but it's a start.
 */
public interface Check {

    String getShortName();

    // TODO This may not be enough, because Checkstyle accepts more forms (e.g. FQCN without "Check" postfix).
    String getFullyQualifiedName();

    /**
     * Configure the check given the CheckStyle configuration.
     *
     * @param config the configuration.
     */
    void configure(@NotNull CheckstyleInternalObject config);

    /**
     * Process a file.
     *
     * @param file the file
     * @param pEventSourceName sourceName of the audit event
     * @return true to continue processing, false to cancel
     */
    boolean process(@NotNull PsiFile file, @NotNull String pEventSourceName);
}
