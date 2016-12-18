package org.infernus.idea.checkstyle.service.cmd;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.infernus.idea.checkstyle.service.entities.CheckerWithConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Destroy a checker instance.
 */
public class OpDestroyChecker
        implements CheckstyleCommand<Void>
{
    private final CheckerWithConfig checkerWithConfig;


    public OpDestroyChecker(@NotNull final CheckstyleInternalObject pCheckerWithConfig) {
        checkerWithConfig = (CheckerWithConfig) pCheckerWithConfig;
    }


    @Nullable
    @Override
    public Void execute(@NotNull final Project pProject) {
        checkerWithConfig.getChecker().destroy();
        return null;
    }
}
