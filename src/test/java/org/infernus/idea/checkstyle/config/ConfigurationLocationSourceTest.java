package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.TestHelper;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.SortedSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
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
}
