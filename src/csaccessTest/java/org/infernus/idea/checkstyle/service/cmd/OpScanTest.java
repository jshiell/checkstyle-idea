package org.infernus.idea.checkstyle.service.cmd;

import java.util.Collections;
import java.util.Optional;

import com.intellij.openapi.project.Project;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.infernus.idea.checkstyle.exception.CheckstyleVersionMixException;
import org.infernus.idea.checkstyle.service.entities.CheckerWithConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class OpScanTest {
    private static final Project PROJECT = Mockito.mock(Project.class);

    private static final class InvalidObject
            implements CheckstyleInternalObject {
        // does not matter
    }


    @Test
    public void testWrongCheckerClass() {
        assertThrows(CheckstyleVersionMixException.class,
                () -> new OpScan(new InvalidObject(), Collections.emptyList(), false, 2, Optional.empty()));
    }


    @Test
    public void testEmptyListOfFiles() throws CheckstyleException {
        OpScan cmd = new OpScan(new CheckerWithConfig(null, null), Collections.emptyList(), false, 2, Optional.empty());
        assertEquals(Collections.emptyMap(), cmd.execute(PROJECT));
    }
}
