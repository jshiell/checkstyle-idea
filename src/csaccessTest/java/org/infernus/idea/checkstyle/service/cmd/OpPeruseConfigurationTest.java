package org.infernus.idea.checkstyle.service.cmd;


import com.intellij.openapi.project.Project;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.infernus.idea.checkstyle.exception.CheckstyleVersionMixException;
import org.infernus.idea.checkstyle.service.entities.CsConfigObject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class OpPeruseConfigurationTest
{
    private static final Project PROJECT = Mockito.mock(Project.class);

    private static final class InvalidObject implements CheckstyleInternalObject
    {
        // does not matter
    }


    @Test(expected = CheckstyleVersionMixException.class)
    public void testWrongConfigurationClass() {
        //noinspection ConstantConditions
        new OpPeruseConfiguration(new InvalidObject(), null);
    }


    @Test
    public void testNullConfig() throws CheckstyleException {
        //noinspection ConstantConditions
        final OpPeruseConfiguration cmd = new OpPeruseConfiguration(new CsConfigObject(null), null);
        Assert.assertNull(cmd.execute(PROJECT));
    }
}
