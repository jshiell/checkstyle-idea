package org.infernus.idea.checkstyle;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.csapi.ConfigurationModule;
import org.infernus.idea.checkstyle.model.BundledConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.TreeSet;

import static org.hamcrest.Matchers.*;
import static org.infernus.idea.checkstyle.csapi.BundledConfig.GOOGLE_CHECKS;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class CheckstylePluginApiTest {
    private static final String CHECKSTYLE_VERSION = "7.1.2";

    private CheckstylePluginApi underTest;

    private CheckstyleProjectService checkstyleProjectService;
    private PluginConfigurationManager pluginConfigManager;
    private Project project;

    @Before
    public void setUp() {
        project = mock(Project.class);

        pluginConfigManager = mock(PluginConfigurationManager.class);
        when(pluginConfigManager.getCurrent())
                .thenReturn(PluginConfigurationBuilder.testInstance(CHECKSTYLE_VERSION).build());

        checkstyleProjectService = new CheckstyleProjectService(project, pluginConfigManager);
        underTest = new CheckstylePluginApi(new CheckStylePlugin(project, pluginConfigManager), checkstyleProjectService);
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

    @Test
    public void currentConfigurationReturnsWithoutErrorWhenNoConfigurationIsActive() {
        underTest.visitCurrentConfiguration((configName, configModule) -> fail("This should not be invoked"));
    }

    @Test
    public void currentConfigurationCanBeVisited() {
        BundledConfigurationLocation googleChecks = new ConfigurationLocationFactory().create(GOOGLE_CHECKS, project);
        TreeSet<ConfigurationLocation> locations = new TreeSet<>();
        locations.add(googleChecks);
        when(pluginConfigManager.getCurrent())
                .thenReturn(PluginConfigurationBuilder
                        .testInstance(CHECKSTYLE_VERSION)
                        .withLocations(locations)
                        .withActiveLocation(googleChecks)
                        .build());
        CheckstylePluginApi.ConfigurationVisitor visitor = mock(CheckstylePluginApi.ConfigurationVisitor.class);

        underTest.visitCurrentConfiguration(visitor);

        ArgumentCaptor<ConfigurationModule> configModuleCaptor = ArgumentCaptor.forClass(ConfigurationModule.class);
        verify(visitor, times(56)).accept(eq("Google Checks"), configModuleCaptor.capture());

        assertThat(configModuleCaptor.getValue(), is(not(nullValue())));
        assertThat(configModuleCaptor.getValue().getName(), is("CommentsIndentation"));

    }
}
