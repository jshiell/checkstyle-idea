package org.infernus.idea.checkstyle.service.cmd;

import java.util.Collections;
import java.util.Optional;

import com.intellij.openapi.project.Project;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.infernus.idea.checkstyle.exception.CheckstyleVersionMixException;
import org.infernus.idea.checkstyle.service.entities.CheckerWithConfig;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;


public class OpScanTest {
    private static final Project PROJECT = Mockito.mock(Project.class);

    private static final class InvalidObject
            implements CheckstyleInternalObject {
        // does not matter
    }


    @Test(expected = CheckstyleVersionMixException.class)
    public void testWrongCheckerClass() {
        new OpScan(new InvalidObject(), Collections.emptyList(), false, 2, Optional.empty());
    }


    @Test
    public void testEmptyListOfFiles() throws CheckstyleException {
        OpScan cmd = new OpScan(new CheckerWithConfig(null, null), Collections.emptyList(), false, 2, Optional.empty());
        Assert.assertEquals(Collections.emptyMap(), cmd.execute(PROJECT));
    }
}
