package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PluginConfigurationManagerListenerTest {

    private final Project project = mock(Project.class);
    private final ProjectConfigurationState projectConfigurationState = mock(ProjectConfigurationState.class);
    private final PluginConfigurationManager manager = new PluginConfigurationManager(project, Runnable::run);

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

    @Test
    void configurationChangedIsDispatchedRatherThanRunSynchronously() {
        final List<Runnable> dispatched = new ArrayList<>();
        final PluginConfigurationManager dispatchingManager =
                new PluginConfigurationManager(project, dispatched::add);
        final AtomicInteger callCount = new AtomicInteger();
        dispatchingManager.addConfigurationListener(callCount::incrementAndGet);

        dispatchingManager.setCurrent(mock(PluginConfiguration.class), true);
        assertEquals(0, callCount.get(), "listener must not run synchronously on the calling thread");

        dispatched.forEach(Runnable::run);
        assertEquals(1, callCount.get(), "listener must run once the dispatched callback executes");
    }
}
