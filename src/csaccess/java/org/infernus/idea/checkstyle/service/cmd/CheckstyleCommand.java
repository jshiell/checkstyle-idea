package org.infernus.idea.checkstyle.service.cmd;

import com.intellij.openapi.project.Project;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import org.jetbrains.annotations.NotNull;

/**
 * Describes commands of the Checkstyle access layer.
 *
 * @param <R> result type
 */
public interface CheckstyleCommand<R> {
    /**
     * Execute the command.
     *
     * @param project the IntelliJ project in whose context we are executing
     * @return the result of the command
     * @throws CheckstyleException if an exception was thrown from the Checkstyle tool
     */
    R execute(@NotNull Project project) throws CheckstyleException;
}
