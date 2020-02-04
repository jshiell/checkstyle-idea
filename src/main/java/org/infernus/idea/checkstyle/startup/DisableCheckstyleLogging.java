package org.infernus.idea.checkstyle.startup;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;

public class DisableCheckstyleLogging implements StartupActivity {

    private static final Logger LOG = Logger.getInstance(DisableCheckstyleLogging.class);

    @Override
    public void runActivity(@NotNull final Project project) {
        try {
            // This is a nasty hack to get around IDEA's DialogAppender sending any errors to the Event Log,
            // which would result in CheckStyle parse errors spamming the Event Log.
            org.apache.log4j.Logger.getLogger("com.puppycrawl.tools.checkstyle.TreeWalker").setLevel(Level.OFF);
        } catch (Exception e) {
            LOG.warn("Unable to suppress logging from CheckStyle's TreeWalker", e);
        }
    }

}
