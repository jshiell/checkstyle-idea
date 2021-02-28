package org.infernus.idea.checkstyle.service.cmd;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.infernus.idea.checkstyle.exception.CheckstyleVersionMixException;
import org.infernus.idea.checkstyle.service.entities.HasChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * Destroy a checker instance.
 */
public class OpDestroyChecker
        implements CheckstyleCommand<Void> {

    private static final Logger LOG = Logger.getInstance(OpDestroyChecker.class);

    private final HasChecker hasChecker;

    public OpDestroyChecker(@NotNull final CheckstyleInternalObject checker) {
        if (!(checker instanceof HasChecker)) {
            throw new CheckstyleVersionMixException(HasChecker.class, checker);
        }
        this.hasChecker = (HasChecker) checker;
    }

    @Nullable
    @Override
    public Void execute(@NotNull final Project project) {
        try {
            if (hasChecker.getCheckerLock().tryLock(1, TimeUnit.SECONDS)) {
                try {
                    hasChecker.getChecker().destroy();
                } finally {
                    hasChecker.getCheckerLock().unlock();
                }
            }
        } catch (InterruptedException e) {
            LOG.debug("Checker will not be destroyed as we couldn't lock the checker", e);
            Thread.currentThread().interrupt();
        }
        return null;
    }
}
