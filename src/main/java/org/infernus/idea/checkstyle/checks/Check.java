package org.infernus.idea.checkstyle.checks;

import com.intellij.psi.PsiFile;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.jetbrains.annotations.NotNull;

/**
 * Allows extra logic for a certain Check.
 * <p/>
 * Not a lot of extra logic at present, but it's a start.
 */
public interface Check {

    /**
     * Configure the check given the CheckStyle configuration.
     *
     * @param config the configuration.
     */
    void configure(@NotNull Configuration config);

    /**
     * Process a file.
     *
     * @param file  the file.
     * @param event the audit event.
     * @return true to continue processing, false to cancel.
     */
    boolean process(@NotNull PsiFile file, @NotNull AuditEvent event);

}
