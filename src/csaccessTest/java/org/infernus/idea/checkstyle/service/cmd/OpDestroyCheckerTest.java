package org.infernus.idea.checkstyle.service.cmd;

import com.intellij.openapi.project.Project;
import com.puppycrawl.tools.checkstyle.Checker;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.infernus.idea.checkstyle.exception.CheckstyleVersionMixException;
import org.infernus.idea.checkstyle.service.CheckstyleActionsImpl;
import org.infernus.idea.checkstyle.service.entities.CheckerWithConfig;
import org.junit.Test;
import org.mockito.Mockito;


public class OpDestroyCheckerTest
{
    private static final Project PROJECT = Mockito.mock(Project.class);

    private static class WrongObject
            implements CheckstyleInternalObject
    {
        // does not matter
    }


    @Test
    public void testDestroyChecker() {
        CheckerWithConfig checkerWithConfig = new CheckerWithConfig(new Checker(), null);
        new CheckstyleActionsImpl(PROJECT).destroyChecker(checkerWithConfig);
    }


    @Test(expected = CheckstyleVersionMixException.class)
    public void testMixExceptionInInit() {
        new OpDestroyChecker(new WrongObject());
    }


    @Test(expected = CheckstyleVersionMixException.class)
    public void testCheckerNull() {
        //noinspection ConstantConditions
        new OpDestroyChecker(null);
    }
}
