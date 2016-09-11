package org.infernus.idea.checkstyle;

import java.util.SortedSet;

import com.intellij.openapi.project.Project;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;


public class CheckstyleProjectServiceTest
{
    @Test
    public void testReadVersions()
    {
        Project mockProject = Mockito.mock(Project.class);
        CheckstyleProjectService service = new CheckstyleProjectService(mockProject);
        SortedSet<String> versions = service.getSupportedVersions();
        Assert.assertNotNull(versions);
        Assert.assertTrue(versions.size() > 0);
        Assert.assertNotNull(versions.comparator());
        Assert.assertEquals(VersionComparator.class, versions.comparator().getClass());
    }
}
