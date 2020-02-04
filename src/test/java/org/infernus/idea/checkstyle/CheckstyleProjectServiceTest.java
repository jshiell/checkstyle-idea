package org.infernus.idea.checkstyle;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.junit.Before;
import org.junit.Test;
import org.picocontainer.PicoContainer;

import java.util.SortedSet;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class CheckstyleProjectServiceTest {
    private static final String CHECKSTYLE_VERSION = "7.1.2";

    private CheckstyleProjectService underTest;

    @Before
    public void setUp() {
        Project project = mock(Project.class);

        PicoContainer picoContainer = mock(PicoContainer.class);
        when(project.getPicoContainer()).thenReturn(picoContainer);

        PluginConfigurationManager pluginConfigManager = mock(PluginConfigurationManager.class);
        when(pluginConfigManager.getCurrent())
                .thenReturn(PluginConfigurationBuilder.testInstance(CHECKSTYLE_VERSION).build());
        when(picoContainer.getComponentInstance(PluginConfigurationManager.class.getName())).thenReturn(pluginConfigManager);

        underTest = new CheckstyleProjectService(project);
    }

    @Test
    public void readingSupportedVersionsReturnsASetOfVersions() {
        SortedSet<String> versions = underTest.getSupportedVersions();
        assertThat(versions, hasItem(CHECKSTYLE_VERSION));
        assertThat(versions.comparator(), is(instanceOf(VersionComparator.class)));
    }

    @Test
    public void classLoaderCanBeRetrievedByExternalTools() {
        underTest.activateCheckstyleVersion(CHECKSTYLE_VERSION, null);
        assertThat(underTest.underlyingClassLoader(), is(not(nullValue())));
    }

    @Test
    public void classLoaderCanLoadCheckStyleInternalClasses() throws ClassNotFoundException {
        underTest.activateCheckstyleVersion(CHECKSTYLE_VERSION, null);
        assertThat(underTest.underlyingClassLoader().loadClass("com.puppycrawl.tools.checkstyle.Checker"),
                is(not(nullValue())));
    }
}
