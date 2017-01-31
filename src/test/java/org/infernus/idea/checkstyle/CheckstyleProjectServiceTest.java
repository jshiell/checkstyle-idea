package org.infernus.idea.checkstyle;

import com.intellij.openapi.project.Project;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.SortedSet;


public class CheckstyleProjectServiceTest
{
    private static final Project PROJECT = Mockito.mock(Project.class);


    @BeforeClass
    public static void setUp() {
        CheckStyleConfiguration mockPluginConfig = Mockito.mock(CheckStyleConfiguration.class);
        Mockito.when(mockPluginConfig.getCheckstyleVersion(Mockito.anyString())).thenReturn("7.1.1");
        Mockito.when(mockPluginConfig.getThirdPartyClassPath()).thenReturn(null);
        Mockito.when(mockPluginConfig.getProject()).thenReturn(PROJECT);
        CheckStyleConfiguration.activateMock4UnitTesting(mockPluginConfig);
    }

    @AfterClass
    public static void tearDown() {
        CheckStyleConfiguration.activateMock4UnitTesting(null);
    }


    @Test
    public void testReadVersions() {
        CheckstyleProjectService service = new CheckstyleProjectService(PROJECT);
        SortedSet<String> versions = service.getSupportedVersions();
        Assert.assertNotNull(versions);
        Assert.assertTrue(versions.size() > 0);
        Assert.assertNotNull(versions.comparator());
        Assert.assertEquals(VersionComparator.class, versions.comparator().getClass());
    }
}
