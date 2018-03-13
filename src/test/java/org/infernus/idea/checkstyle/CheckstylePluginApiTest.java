package org.infernus.idea.checkstyle;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CheckstylePluginApiTest {
    private static final String CHECKSTYLE_VERSION = "7.1.1";

    private CheckstylePluginApi underTest;
    private CheckstyleProjectService checkstyleProjectService;

    @Before
    public void setUp() {
        PluginConfigurationManager pluginConfigManager = mock(PluginConfigurationManager.class);
        when(pluginConfigManager.getCurrent())
                .thenReturn(PluginConfigurationBuilder.testInstance(CHECKSTYLE_VERSION).build());
        checkstyleProjectService = new CheckstyleProjectService(mock(Project.class), pluginConfigManager);
        underTest = new CheckstylePluginApi(checkstyleProjectService);
    }

    @Test
    public void classLoaderCanBeRetrievedByExternalTools() {
        checkstyleProjectService.activateCheckstyleVersion(CHECKSTYLE_VERSION, null);
        assertThat(underTest.currentCheckstyleClassLoader(), is(not(nullValue())));
    }

    @Test
    public void classLoaderCanLoadCheckStyleInternalClasses() throws ClassNotFoundException {
        checkstyleProjectService.activateCheckstyleVersion(CHECKSTYLE_VERSION, null);
        assertThat(underTest.currentCheckstyleClassLoader(), is(not(nullValue())));
        assertThat(underTest.currentCheckstyleClassLoader().loadClass("com.puppycrawl.tools.checkstyle.Checker"),
                is(not(nullValue())));
    }
}
