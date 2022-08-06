package org.infernus.idea.checkstyle;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.csapi.ConfigurationModule;
import org.infernus.idea.checkstyle.model.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.infernus.idea.checkstyle.csapi.BundledConfig.GOOGLE_CHECKS;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class CheckstylePluginApiTest {
    private static final String CHECKSTYLE_VERSION = "8.0";

    private CheckstylePluginApi underTest;

    private CheckstyleProjectService checkstyleProjectService;
    private PluginConfigurationManager pluginConfigManager;
    private ConfigurationLocationFactory configurationLocationFactory;
    private Project project;

    @Before
    public void setUp() {
        project = TestHelper.mockProject();

        pluginConfigManager = mock(PluginConfigurationManager.class);
        when(pluginConfigManager.getCurrent())
                .thenReturn(PluginConfigurationBuilder.testInstance(CHECKSTYLE_VERSION).build());
        when(project.getService(PluginConfigurationManager.class)).thenReturn(pluginConfigManager);

        checkstyleProjectService = new CheckstyleProjectService(project);
        when(project.getService(CheckstyleProjectService.class)).thenReturn(checkstyleProjectService);

        configurationLocationFactory = mock(ConfigurationLocationFactory.class);
        when(project.getService(ConfigurationLocationFactory.class)).thenReturn(configurationLocationFactory);

        underTest = new CheckstylePluginApi(project);
    }

    @Test
    public void classLoaderCanBeRetrievedByExternalTools() {
        checkstyleProjectService.activateCheckstyleVersion(CHECKSTYLE_VERSION, null);
        assertThat(underTest.currentCheckstyleClassLoader(), is(not(nullValue())));
    }

    @Test
    public void classLoaderCanLoadCheckStyleInternalClasses() throws ClassNotFoundException {
        checkstyleProjectService.activateCheckstyleVersion(CHECKSTYLE_VERSION, null);
        ClassLoader currentClassLoader = underTest.currentCheckstyleClassLoader();
        assertThat(currentClassLoader, is(not(nullValue())));
        assertThat(currentClassLoader.loadClass("com.puppycrawl.tools.checkstyle.Checker"),
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
                        .withActiveLocationIds(locations.stream()
                                .map(ConfigurationLocation::getId)
                                .collect(Collectors.toCollection(TreeSet::new)))
                        .build());
        CheckstylePluginApi.ConfigurationVisitor visitor = mock(CheckstylePluginApi.ConfigurationVisitor.class);
        NamedScope allScope = NamedScopeHelper.getScopeByIdWithDefaultFallback(project, "All");
        when(configurationLocationFactory.create(eq(project), anyString(), eq(ConfigurationType.BUNDLED), eq("(bundled)"), eq("Google Checks"), eq(allScope)))
                .thenReturn(googleChecks);

        underTest.visitCurrentConfiguration(visitor);

        ArgumentCaptor<ConfigurationModule> configModuleCaptor = ArgumentCaptor.forClass(ConfigurationModule.class);
        verify(visitor, times(58)).accept(eq("Google Checks"), configModuleCaptor.capture());

        assertThat(configModuleCaptor.getValue(), is(not(nullValue())));
        assertThat(configModuleCaptor.getValue().getName(), is("CommentsIndentation"));

    }
}
