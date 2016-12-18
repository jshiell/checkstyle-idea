package org.infernus.idea.checkstyle.checks;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import com.intellij.openapi.project.Project;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.jetbrains.annotations.NotNull;


/**
 * Factory for producing various check modifications.
 */
public final class CheckFactory
{
    private static final Log LOG = LogFactory.getLog(CheckFactory.class);

    private static final Class<?>[] CHECK_CLASSES = {JavadocPackageCheck.class, PackageHtmlCheck.class};

    private CheckFactory() {
    }

    @NotNull
    public static List<Check> getChecks(final Project pProject, final CheckstyleInternalObject config) {
        final List<Check> checks = new ArrayList<>();

        for (final Class<?> checkClass : CHECK_CLASSES) {
            try {
                Constructor<?> constructor = checkClass.getConstructor(Project.class);
                final Check check = (Check) constructor.newInstance(pProject);
                check.configure(config);
                checks.add(check);
            } catch (Exception e) {
                LOG.error("Couldn't instantiate check class " + checkClass, e);
            }
        }

        return checks;
    }
}
