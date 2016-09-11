package org.infernus.idea.checkstyle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;


public class Checkstyle
{
    private static final Log LOG = LogFactory.getLog(CheckStylePlugin.class);



    public void disableCheckstyleLogging()
    {
        try {
            // This is a nasty hack to get around IDEA's DialogAppender sending any errors to the Event Log,
            // which would result in CheckStyle parse errors spamming the Event Log.
            Logger.getLogger(CheckstyleClasses.TreeWalker.getFqcn()).setLevel(Level.OFF);
        }
        catch (Exception e) {
            LOG.error("Unable to suppress logging from CheckStyle's TreeWalker", e);
        }
    }
}
