package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.TestHelper;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigurationLocationSourceTest {

    private final Project project = TestHelper.mockProject();
    private final Module module = mock(Module.class);
    private final ModuleConfigurationState moduleConfiguration = mock(ModuleConfigurationState.class);

    @BeforeEach
    void setUp() {
        when(module.getService(ModuleConfigurationState.class)).thenReturn(moduleConfiguration);
    }

    @Test
    void excludedModuleReturnsEmptyEvenWhenOverrideProvided() {
        when(moduleConfiguration.isExcluded()).thenReturn(true);
        ConfigurationLocation override = mock(ConfigurationLocation.class);

        SortedSet<ConfigurationLocation> result = new ConfigurationLocationSource(project)
                .getConfigurationLocations(module, override);

        assertThat(result, is(empty()));
    }

    @Test
    void overrideIsReturnedWhenModuleIsNotExcluded() {
        when(moduleConfiguration.isExcluded()).thenReturn(false);
        when(moduleConfiguration.getActiveLocationIds()).thenReturn(new java.util.TreeSet<>());
        ConfigurationLocation override = mock(ConfigurationLocation.class);
        when(override.compareTo(override)).thenReturn(0);

        PluginConfigurationManager configManager = mock(PluginConfigurationManager.class);
        PluginConfiguration pluginConfig = mock(PluginConfiguration.class);
        when(project.getService(PluginConfigurationManager.class)).thenReturn(configManager);
        when(configManager.getCurrent()).thenReturn(pluginConfig);
        when(pluginConfig.getActiveLocations()).thenReturn(new java.util.TreeSet<>());

        SortedSet<ConfigurationLocation> result = new ConfigurationLocationSource(project)
                .getConfigurationLocations(module, override);

        assertThat(result, contains(override));
    }

    @Test
    void moduleActiveLocationsWinWhenPresent() {
        when(moduleConfiguration.isExcluded()).thenReturn(false);
        when(moduleConfiguration.getActiveLocationIds()).thenReturn(new TreeSet<>(List.of("loc-1")));

        ConfigurationLocation location = aLocation("loc-1", "Module");
        PluginConfigurationManager configManager = mock(PluginConfigurationManager.class);
        PluginConfiguration pluginConfig = mock(PluginConfiguration.class);
        when(project.getService(PluginConfigurationManager.class)).thenReturn(configManager);
        when(configManager.getCurrent()).thenReturn(pluginConfig);
        when(pluginConfig.getLocationById("loc-1")).thenReturn(Optional.of(location));

        SortedSet<ConfigurationLocation> result = new ConfigurationLocationSource(project)
                .getConfigurationLocations(module, null);

        assertThat(result, contains(location));
    }

    @Test
    void fallsBackToGlobalLocationsWhenProjectHasNoActiveLocations() {
        PluginConfigurationManager configManager = mock(PluginConfigurationManager.class);
        PluginConfiguration pluginConfig = mock(PluginConfiguration.class);
        when(project.getService(PluginConfigurationManager.class)).thenReturn(configManager);
        when(configManager.getCurrent()).thenReturn(pluginConfig);
        when(pluginConfig.getActiveLocations()).thenReturn(new TreeSet<>());

        ConfigurationLocationFactory locationFactory = mock(ConfigurationLocationFactory.class);
        when(project.getService(ConfigurationLocationFactory.class)).thenReturn(locationFactory);
        ConfigurationLocation created = mock(ConfigurationLocation.class);
        when(created.compareTo(any())).thenReturn(0);
        when(locationFactory.create(project, "glob-1", ConfigurationType.LOCAL_FILE, "c:/rules.xml", "Global", null))
                .thenReturn(created);

        ApplicationConfigurationState appState = new ApplicationConfigurationState();
        ApplicationConfigurationState.GlobalConfigurationLocation dto =
                new ApplicationConfigurationState.GlobalConfigurationLocation(
                        "glob-1", "LOCAL_FILE", "  c:/rules.xml  ", "Global", null);
        dto.properties = java.util.Map.of("k", "v");
        appState.setUseGlobalRulesByDefault(true);
        appState.setGlobalLocations(List.of(dto));
        appState.setActiveGlobalLocationIds(List.of("glob-1"));

        Application app = mock(Application.class);
        when(app.getService(ApplicationConfigurationState.class)).thenReturn(appState);

        try (MockedStatic<ApplicationManager> ignored = org.mockito.Mockito.mockStatic(ApplicationManager.class)) {
            ignored.when(ApplicationManager::getApplication).thenReturn(app);

            SortedSet<ConfigurationLocation> result = new ConfigurationLocationSource(project)
                    .getConfigurationLocations(null, null);

            assertThat(result, contains(created));
            verify(created).setProperties(java.util.Map.of("k", "v"));
            verify(created).setNamedScope(TestHelper.NAMED_SCOPE);
        }
    }

    @Test
    void returnsEmptyWhenGlobalFallbackDisabled() {
        PluginConfigurationManager configManager = mock(PluginConfigurationManager.class);
        PluginConfiguration pluginConfig = mock(PluginConfiguration.class);
        when(project.getService(PluginConfigurationManager.class)).thenReturn(configManager);
        when(configManager.getCurrent()).thenReturn(pluginConfig);
        when(pluginConfig.getActiveLocations()).thenReturn(new TreeSet<>());

        ApplicationConfigurationState appState = new ApplicationConfigurationState();
        appState.setUseGlobalRulesByDefault(false);

        Application app = mock(Application.class);
        when(app.getService(ApplicationConfigurationState.class)).thenReturn(appState);

        try (MockedStatic<ApplicationManager> ignored = org.mockito.Mockito.mockStatic(ApplicationManager.class)) {
            ignored.when(ApplicationManager::getApplication).thenReturn(app);

            SortedSet<ConfigurationLocation> result = new ConfigurationLocationSource(project)
                    .getConfigurationLocations(null, null);

            assertThat(result, is(empty()));
        }
    }

    private ConfigurationLocation aLocation(final String id, final String description) {
        ConfigurationLocation loc = new ConfigurationLocation(id, ConfigurationType.LOCAL_FILE, project) {
            @Override
            @NotNull
            protected InputStream resolveFile(@NotNull final ClassLoader checkstyleClassLoader) {
                return new ByteArrayInputStream(new byte[0]);
            }

            @Override
            public Object clone() {
                return aLocation(id, description);
            }
        };
        loc.setDescription(description);
        return loc;
    }
}
