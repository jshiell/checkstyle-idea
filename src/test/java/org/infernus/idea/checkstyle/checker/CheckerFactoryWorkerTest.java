package org.infernus.idea.checkstyle.checker;

import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.csapi.CheckstyleActions;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckerFactoryWorkerTest {

    @Mock
    private ConfigurationLocation location;

    @Mock
    private CheckstyleProjectService checkstyleProjectService;

    @Mock
    private CheckstyleActions checkstyleActions;

    @Test
    void callDelegatesToCheckstyleInstanceAndWrapsResult() throws Exception {
        CheckStyleChecker mockChecker = mock(CheckStyleChecker.class);
        when(checkstyleProjectService.getCheckstyleInstance()).thenReturn(checkstyleActions);
        when(checkstyleActions.createChecker(any(), eq(location), any(Map.class))).thenReturn(mockChecker);

        CheckerFactoryWorker worker = new CheckerFactoryWorker(location, Map.of(), null, checkstyleProjectService);
        CachedChecker result = worker.call();

        assertThat(result, notNullValue());
        assertThat(result.getCheckStyleChecker(), notNullValue());
    }

    @Test
    void callPassesPropertiesAndModuleToCheckstyleInstance() throws Exception {
        CheckStyleChecker mockChecker = mock(CheckStyleChecker.class);
        Map<String, String> props = Map.of("prop1", "val1");
        when(checkstyleProjectService.getCheckstyleInstance()).thenReturn(checkstyleActions);
        when(checkstyleActions.createChecker(eq(null), eq(location), eq(props))).thenReturn(mockChecker);

        CheckerFactoryWorker worker = new CheckerFactoryWorker(location, props, null, checkstyleProjectService);
        CachedChecker result = worker.call();

        assertThat(result, notNullValue());
    }
}
