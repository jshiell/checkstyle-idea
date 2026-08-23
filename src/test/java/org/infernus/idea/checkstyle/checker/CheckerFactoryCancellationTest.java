package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.StringConfigurationLocation;
import org.infernus.idea.checkstyle.TestHelper;
import org.infernus.idea.checkstyle.csapi.CheckstyleActions;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Interrupting the thread that called {@code CheckerFactory.checker(...)} must not leave the
 * worker thread running the Checkstyle call behind it: {@code executeWorker} now cancels the
 * inner future on interrupt, which interrupts whatever the worker thread is doing.
 */
@ExtendWith(MockitoExtension.class)
class CheckerFactoryCancellationTest {

    private Project project;
    private CheckerFactoryCache cache;
    private ConfigurationLocation location;

    @Mock
    private CheckstyleProjectService checkstyleProjectService;

    private final CountDownLatch workerStarted = new CountDownLatch(1);
    private final CountDownLatch workerInterrupted = new CountDownLatch(1);
    private final AtomicReference<Thread> workerThread = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        project = TestHelper.mockProject();
        cache = new CheckerFactoryCache();
        location = new StringConfigurationLocation("<module name=\"Checker\"/>", project);

        when(checkstyleProjectService.underlyingClassLoader()).thenReturn(getClass().getClassLoader());
        final CheckstyleActions checkstyleActions = mock(CheckstyleActions.class);
        when(checkstyleActions.createChecker(any(), any(), any())).thenAnswer(invocation -> {
            workerThread.set(Thread.currentThread());
            workerStarted.countDown();
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                workerInterrupted.countDown();
                throw new RuntimeException(e);
            }
            return mock(CheckStyleChecker.class);
        });
        when(checkstyleProjectService.getCheckstyleInstance()).thenReturn(checkstyleActions);
    }

    @Test
    void interruptingTheCallingThreadInterruptsTheInFlightWorker() throws InterruptedException {
        final CheckerFactory factory = CheckerFactory.create(project, checkstyleProjectService, cache);

        final Thread caller = new Thread(() -> {
            try {
                factory.checker(null, location);
            } catch (RuntimeException expected) {
                // pre-existing behaviour on interrupt: the InterruptedException falls through to
                // blockAndShowException, blocking the rules file - out of scope for this test,
                // which only cares that the worker thread was actually interrupted
            }
        });
        caller.start();

        assertTrue(workerStarted.await(2, TimeUnit.SECONDS), "worker never started");
        caller.interrupt();

        assertTrue(workerInterrupted.await(2, TimeUnit.SECONDS),
                "worker thread was not interrupted after the calling thread was interrupted");
        caller.join(2_000);
        assertFalse(caller.isAlive(), "calling thread did not return after interrupt");
    }

    @Test
    void theWorkerRunsOnANamedDaemonThread() throws InterruptedException {
        final CheckerFactory factory = CheckerFactory.create(project, checkstyleProjectService, cache);

        final Thread caller = new Thread(() -> {
            try {
                factory.checker(null, location);
            } catch (RuntimeException expected) {
                // pre-existing behaviour on interrupt: the InterruptedException falls through to
                // blockAndShowException, blocking the rules file - out of scope for this test,
                // which only cares that the worker thread was actually interrupted
            }
        });
        caller.start();

        assertTrue(workerStarted.await(2, TimeUnit.SECONDS), "worker never started");
        caller.interrupt();
        caller.join(2_000);

        assertTrue(workerThread.get().getName().contains("CheckerFactoryWorker"),
                "worker thread name does not identify its origin: " + workerThread.get().getName());
        assertTrue(workerThread.get().isDaemon(), "worker thread is not a daemon thread");
    }
}
