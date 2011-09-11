package org.infernus.idea.checkstyle.checks;

import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for producing various check modifications.
 */
public final class CheckFactory {

    private static final Log LOG = LogFactory.getLog(CheckFactory.class);

    private static final Class[] CHECK_CLASSES = {JavadocPackageCheck.class, PackageHtmlCheck.class};

    private CheckFactory() {
    }

    public static List<Check> getChecks(final Configuration config) {
        final List<Check> checks = new ArrayList<Check>();

        for (final Class checkClass : CHECK_CLASSES) {
            try {
                final Check check = (Check) checkClass.newInstance();
                check.configure(config);
                checks.add(check);

            } catch (Exception e) {
                LOG.error("Couldn't instantiate check class " + checkClass, e);
            }
        }

        return checks;
    }

}
