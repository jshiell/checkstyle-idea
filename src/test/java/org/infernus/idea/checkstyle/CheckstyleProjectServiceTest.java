package org.infernus.idea.checkstyle;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.config.PluginConfiguration;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.junit.Before;
import org.junit.Test;

import java.util.SortedSet;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class CheckstyleProjectServiceTest {
    private static final String CHECKSTYLE_VERSION = "7.1.1";
    private static final Project PROJECT = mock(Project.class);

    private CheckstyleProjectService underTest;

    @Before
    public void setUp() {
        PluginConfigurationManager mockPluginConfig = mock(PluginConfigurationManager.class);
        final PluginConfiguration mockConfigDto = PluginConfigurationBuilder.testInstance(CHECKSTYLE_VERSION).build();
        when(mockPluginConfig.getCurrent()).thenReturn(mockConfigDto);
        underTest = new CheckstyleProjectService(PROJECT, mockPluginConfig);
    }

    @Test
    public void testReadVersions() {
        SortedSet<String> versions = underTest.getSupportedVersions();
        assertNotNull(versions);
        assertTrue(versions.size() > 0);
        assertNotNull(versions.comparator());
        assertEquals(VersionComparator.class, versions.comparator().getClass());
    }

    @Test
    public void testCanLoadClassLoader() {
        underTest.activateCheckstyleVersion(CHECKSTYLE_VERSION, null);
        assertNotNull(underTest.getUnderlyingClassLoader());
    }

    @Test
    public void classLoaderCanLoadCheckStyleInternalClasses() throws ClassNotFoundException {
        underTest.activateCheckstyleVersion(CHECKSTYLE_VERSION, null);
        assertNotNull(underTest.getUnderlyingClassLoader().loadClass("com.puppycrawl.tools.checkstyle.Checker"));
    }
}
