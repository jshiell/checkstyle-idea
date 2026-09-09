package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.TestHelper;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PluginConfigurationManagerTest {

    private Project project;
    private ProjectConfigurationState projectConfigurationState;
    private PluginConfigurationManager manager;

    @BeforeEach
    void setUp() {
        project = TestHelper.mockProject();
        projectConfigurationState = mock(ProjectConfigurationState.class);
        when(project.getService(ProjectConfigurationState.class)).thenReturn(projectConfigurationState);
        when(project.getService(ConfigurationLocationFactory.class)).thenReturn(new ConfigurationLocationFactory());
        doNothing().when(projectConfigurationState).setCurrentConfig(any(PluginConfiguration.class));
        manager = new PluginConfigurationManager(project);
    }

    @Test
    void getCurrentCachesLoadedConfiguration() {
        PluginConfiguration loaded = PluginConfigurationBuilder.testInstance("10.0.0").build();
        when(projectConfigurationState.populate(any(PluginConfigurationBuilder.class)))
                .thenReturn(PluginConfigurationBuilder.from(loaded));

        PluginConfiguration first = manager.getCurrent();
        PluginConfiguration second = manager.getCurrent();

        assertSame(first, second);
        verify(projectConfigurationState, times(1)).populate(any(PluginConfigurationBuilder.class));
    }

    @Test
    void setCurrentWithFireEventsTrueNotifiesListeners() {
        AtomicInteger callbackCount = new AtomicInteger(0);
        manager.addConfigurationListener(callbackCount::incrementAndGet);

        manager.setCurrent(PluginConfigurationBuilder.testInstance("10.0.0").build(), true);

        assertEquals(1, callbackCount.get());
    }

    @Test
    void setCurrentWithFireEventsFalseDoesNotNotifyListeners() {
        AtomicInteger callbackCount = new AtomicInteger(0);
        manager.addConfigurationListener(callbackCount::incrementAndGet);

        manager.setCurrent(PluginConfigurationBuilder.testInstance("10.0.0").build(), false);

        assertEquals(0, callbackCount.get());
    }

    @Test
    void invalidateClearsCacheAndNotifiesListeners() {
        PluginConfiguration loaded = PluginConfigurationBuilder.testInstance("10.0.0").build();
        when(projectConfigurationState.populate(any(PluginConfigurationBuilder.class)))
                .thenReturn(PluginConfigurationBuilder.from(loaded));
        manager.getCurrent();

        AtomicInteger callbackCount = new AtomicInteger(0);
        manager.addConfigurationListener(callbackCount::incrementAndGet);

        manager.invalidate();
        manager.getCurrent();

        assertEquals(1, callbackCount.get());
        verify(projectConfigurationState, times(2)).populate(any(PluginConfigurationBuilder.class));
    }

    @Test
    void disableActiveConfigurationPersistsEmptyActiveIds() {
        PluginConfiguration loaded = PluginConfigurationBuilder.testInstance("10.0.0")
                .withActiveLocationIds(new TreeSet<>(java.util.List.of("id-1")))
                .build();
        when(projectConfigurationState.populate(any(PluginConfigurationBuilder.class)))
                .thenReturn(PluginConfigurationBuilder.from(loaded));

        manager.disableActiveConfiguration();

        ArgumentCaptor<PluginConfiguration> captor = ArgumentCaptor.forClass(PluginConfiguration.class);
        verify(projectConfigurationState).setCurrentConfig(captor.capture());
        assertTrue(captor.getValue().getActiveLocationIds().isEmpty());
    }

    @Test
    void addConfigurationListenerIgnoresNull() {
        manager.addConfigurationListener(null);

        manager.setCurrent(PluginConfigurationBuilder.testInstance("10.0.0").build(), true);

        verify(projectConfigurationState).setCurrentConfig(any(PluginConfiguration.class));
    }
}
