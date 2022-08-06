package org.infernus.idea.checkstyle.service.cmd;


import com.intellij.openapi.project.Project;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.infernus.idea.checkstyle.csapi.ConfigVisitor;
import org.infernus.idea.checkstyle.csapi.ConfigurationModule;
import org.infernus.idea.checkstyle.exception.CheckstyleVersionMixException;
import org.infernus.idea.checkstyle.service.entities.CsConfigObject;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertNull;

public class OpPeruseConfigurationTest {
    private static final Project PROJECT = Mockito.mock(Project.class);

    private static final class InvalidObject implements CheckstyleInternalObject {
        // does not matter
    }

    @Test(expected = CheckstyleVersionMixException.class)
    public void testWrongConfigurationClass() {
        new OpPeruseConfiguration(new InvalidObject(), new StubVisitor());
    }

    @Test
    public void testNullConfig() throws CheckstyleException {
        final OpPeruseConfiguration cmd = new OpPeruseConfiguration(new CsConfigObject(null), new StubVisitor());
        assertNull(cmd.execute(PROJECT));
    }

    private static class StubVisitor implements ConfigVisitor {
        @Override
        public void visit(@NotNull final ConfigurationModule module) {

        }
    }
}
