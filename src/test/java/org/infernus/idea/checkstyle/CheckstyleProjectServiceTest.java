package org.infernus.idea.checkstyle;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.config.CheckStyleConfiguration;
import org.infernus.idea.checkstyle.config.PluginConfigDto;
import org.infernus.idea.checkstyle.config.PluginConfigDtoBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.SortedSet;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class CheckstyleProjectServiceTest {
    private static final Project PROJECT = mock(Project.class);


    @BeforeClass
    public static void setUp() {
        CheckStyleConfiguration mockPluginConfig = mock(CheckStyleConfiguration.class);
        final PluginConfigDto mockConfigDto = PluginConfigDtoBuilder.testInstance("7.1.1").build();
        when(mockPluginConfig.getCurrent()).thenReturn(mockConfigDto);
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
        assertNotNull(versions);
        assertTrue(versions.size() > 0);
        assertNotNull(versions.comparator());
        assertEquals(VersionComparator.class, versions.comparator().getClass());
    }
}
