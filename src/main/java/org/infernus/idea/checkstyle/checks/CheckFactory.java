package org.infernus.idea.checkstyle.checks;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;


/**
 * Factory for producing various check modifications.
 */
public final class CheckFactory {

    private static final Logger LOG = Logger.getInstance(CheckFactory.class);

    private static final Class<?>[] CHECK_CLASSES = {JavadocPackageCheck.class, PackageHtmlCheck.class};

    private CheckFactory() {
    }

    @NotNull
    public static List<Check> getChecks(final Project project, final CheckstyleInternalObject config) {
        final List<Check> checks = new ArrayList<>();

        for (final Class<?> checkClass : CHECK_CLASSES) {
            try {
                Constructor<?> constructor = checkClass.getConstructor(Project.class);
                final Check check = (Check) constructor.newInstance(project);
                check.configure(config);
                checks.add(check);
            } catch (Exception e) {
                LOG.warn("Couldn't instantiate check class " + checkClass, e);
            }
        }

        return checks;
    }
}
