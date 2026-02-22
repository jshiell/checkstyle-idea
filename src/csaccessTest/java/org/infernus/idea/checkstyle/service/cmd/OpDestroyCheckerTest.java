package org.infernus.idea.checkstyle.service.cmd;

import com.intellij.openapi.project.Project;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.infernus.idea.checkstyle.exception.CheckstyleVersionMixException;
import org.infernus.idea.checkstyle.service.CheckstyleActionsImpl;
import org.infernus.idea.checkstyle.service.entities.CheckerWithConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;


public class OpDestroyCheckerTest {
    private static final Project PROJECT = mock(Project.class);

    private static final class WrongObject implements CheckstyleInternalObject {
        // does not matter
    }

    @Test
    public void testDestroyChecker() {
        CheckerWithConfig checkerWithConfig = new CheckerWithConfig(new Checker(), new DefaultConfiguration("testConfig"));
        new CheckstyleActionsImpl(PROJECT, mock(CheckstyleProjectService.class)).destroyChecker(checkerWithConfig);
    }

    @Test
    public void testMixExceptionInInit() {
        assertThrows(CheckstyleVersionMixException.class,
                () -> new OpDestroyChecker(new WrongObject()));
    }


    @Test
    public void testCheckerNull() {
        assertThrows(CheckstyleVersionMixException.class,
                () -> {
                    //noinspection ConstantConditions
                    new OpDestroyChecker(null);
                });
    }
}
