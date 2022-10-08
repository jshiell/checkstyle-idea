package org.infernus.idea.checkstyle.checks;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;


/**
 * Factory for producing various check modifications.
 */
public final class CheckFactory {

    private static final Logger LOG = Logger.getInstance(CheckFactory.class);

    private static final List<Function<Project, Check>> CHECKS = Arrays.asList(
            (project) -> new JavadocPackageCheck(project.getService(CheckstyleProjectService.class)),
            (project) -> new PackageHtmlCheck());

    private CheckFactory() {
    }

    @NotNull
    public static List<Check> getChecks(final Project project, final CheckstyleInternalObject config) {
        final List<Check> checks = new ArrayList<>();

        for (final Function<Project, Check> checkFactory : CHECKS) {
            try {
                final Check check = checkFactory.apply(project);
                check.configure(config);
                checks.add(check);
            } catch (Exception e) {
                LOG.warn("Couldn't instantiate check", e);
            }
        }

        return checks;
    }
}
