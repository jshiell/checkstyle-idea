package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PluginConfigurationManagerListenerTest {

    private final Project project = mock(Project.class);
    private final ProjectConfigurationState projectConfigurationState = mock(ProjectConfigurationState.class);
    private final PluginConfigurationManager manager = new PluginConfigurationManager(project);

    @BeforeEach
    void setUp() {
        when(project.getService(ProjectConfigurationState.class)).thenReturn(projectConfigurationState);
    }

    @Test
    void aRemovedListenerReceivesNoCallback() {
        final AtomicInteger callCount = new AtomicInteger();
        final ConfigurationListener listener = callCount::incrementAndGet;

        manager.addConfigurationListener(listener);
        manager.removeConfigurationListener(listener);
        manager.setCurrent(mock(PluginConfiguration.class), true);

        assertEquals(0, callCount.get());
    }

    @Test
    void aRemainingListenerStillReceivesCallbacksAfterAnotherIsRemoved() {
        final AtomicInteger callCount = new AtomicInteger();
        final ConfigurationListener removed = () -> {
            throw new AssertionError("should not be called");
        };
        final ConfigurationListener remaining = callCount::incrementAndGet;

        manager.addConfigurationListener(removed);
        manager.addConfigurationListener(remaining);
        manager.removeConfigurationListener(removed);
        manager.setCurrent(mock(PluginConfiguration.class), true);

        assertEquals(1, callCount.get());
    }

    @Test
    void aListenerThatRemovesItselfDuringTheCallbackDoesNotThrow() {
        final ConfigurationListener[] selfRemoving = new ConfigurationListener[1];
        selfRemoving[0] = () -> manager.removeConfigurationListener(selfRemoving[0]);

        manager.addConfigurationListener(selfRemoving[0]);

        assertDoesNotThrow(() -> manager.setCurrent(mock(PluginConfiguration.class), true));
    }
}
