package org.infernus.idea.checkstyle.service.cmd;

import com.intellij.openapi.project.Project;
import com.puppycrawl.tools.checkstyle.Checker;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.infernus.idea.checkstyle.exception.CheckstyleVersionMixException;
import org.infernus.idea.checkstyle.service.entities.HasChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Destroy a checker instance.
 */
public class OpDestroyChecker
        implements CheckstyleCommand<Void> {

    private final Checker checker;

    public OpDestroyChecker(@NotNull final CheckstyleInternalObject checker) {
        if (!(checker instanceof HasChecker)) {
            throw new CheckstyleVersionMixException(HasChecker.class, checker);
        }
        this.checker = ((HasChecker) checker).getChecker();
    }


    @Nullable
    @Override
    public Void execute(@NotNull final Project project) {
        checker.destroy();
        return null;
    }
}
