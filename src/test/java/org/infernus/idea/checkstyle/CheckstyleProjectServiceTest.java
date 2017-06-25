package org.infernus.idea.checkstyle;

import java.util.Collections;
import java.util.SortedSet;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;


public class CheckstyleProjectServiceTest
{
    private static final Project PROJECT = Mockito.mock(Project.class);


    @BeforeClass
    public static void setUp() {
        CheckStyleConfiguration mockPluginConfig = Mockito.mock(CheckStyleConfiguration.class);
        final PluginConfigDto mockConfigDto = new PluginConfigDto("7.1.1", ScanScope.AllSources, false,
                Collections.emptySortedSet(), Collections.emptyList(), null, false);
        Mockito.when(mockPluginConfig.getCurrentPluginConfig()).thenReturn(mockConfigDto);
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
