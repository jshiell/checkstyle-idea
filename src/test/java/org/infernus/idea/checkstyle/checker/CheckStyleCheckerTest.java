package org.infernus.idea.checkstyle.checker;

import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.csapi.CheckstyleActions;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CheckStyleChecker}.
 *
 * <p>Note: the named-scope filtering path (when a {@code NamedScope} is present) calls
 * {@code ReadAction.compute()} and {@code DependencyValidationManager.getInstance()}, which require
 * a live IntelliJ application context.  That path is therefore covered at the integration level
 * rather than here.
 */
@ExtendWith(MockitoExtension.class)
class CheckStyleCheckerTest {

    @Mock
    private CheckstyleInternalObject checkerWithConfig;

    @Mock
    private CheckstyleActions csServiceInstance;

    @Mock
    private PsiFile psiFile;

    private CheckStyleChecker underTest;

    @BeforeEach
    void setUp() {
        underTest = new CheckStyleChecker(checkerWithConfig, 4, Optional.empty(), csServiceInstance, Optional.empty());
    }

    @Test
    void checkerWithConfigReturnsTheInjectedObject() {
        assertThat(underTest.checkerWithConfig(), is(sameInstance(checkerWithConfig)));
    }

    @Test
    void destroyDelegatesToCsServiceInstance() {
        underTest.destroy();

        verify(csServiceInstance).destroyChecker(checkerWithConfig);
    }

    @Test
    void scanWithEmptyFileListReturnsEmptyMap() {
        Map<PsiFile, List<Problem>> result = underTest.scan(Collections.emptyList(), false);

        assertThat(result.isEmpty(), is(true));
        verifyNoInteractions(csServiceInstance);
    }

    @Test
    void scanDelegatesToCsServiceInstanceWhenNoScopeIsConfigured() {
        ScannableFile scannableFile = mock(ScannableFile.class);
        List<ScannableFile> files = List.of(scannableFile);
        Map<PsiFile, List<Problem>> expected = Map.of(psiFile, List.of());
        when(csServiceInstance.scan(checkerWithConfig, files, false, 4, Optional.empty())).thenReturn(expected);

        Map<PsiFile, List<Problem>> result = underTest.scan(files, false);

        assertThat(result, is(sameInstance(expected)));
    }

    @Test
    void scanPassesSuppressErrorsFlagToCsServiceInstance() {
        ScannableFile scannableFile = mock(ScannableFile.class);
        List<ScannableFile> files = List.of(scannableFile);
        when(csServiceInstance.scan(any(), any(), anyBoolean(), anyInt(), any())).thenReturn(Map.of());

        underTest.scan(files, true);

        verify(csServiceInstance).scan(checkerWithConfig, files, true, 4, Optional.empty());
    }
}
