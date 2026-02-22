package org.infernus.idea.checkstyle.checker;

import com.intellij.psi.PsiFile;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.NamedScopesHolder;
import com.intellij.psi.search.scope.packageSet.PackageSet;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.infernus.idea.checkstyle.csapi.CheckstyleActions;
import org.infernus.idea.checkstyle.csapi.CheckstyleInternalObject;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CheckStyleChecker}.
 *
 * <p>Uses the real headless IDEA application (via {@link BasePlatformTestCase}) so that
 * {@code ReadAction.compute()} and {@code DependencyValidationManager.getInstance()} work
 * correctly when the named-scope filtering path is exercised.</p>
 */
public class CheckStyleCheckerTest extends BasePlatformTestCase {

    private CheckstyleInternalObject checkerWithConfig;
    private CheckstyleActions csServiceInstance;
    private CheckStyleChecker underTest;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        checkerWithConfig = mock(CheckstyleInternalObject.class);
        csServiceInstance = mock(CheckstyleActions.class);
        underTest = new CheckStyleChecker(checkerWithConfig, 4, Optional.empty(), csServiceInstance, Optional.empty());
    }

    public void testCheckerWithConfigReturnsTheInjectedObject() {
        assertSame(checkerWithConfig, underTest.checkerWithConfig());
    }

    public void testDestroyDelegatesToCsServiceInstance() {
        underTest.destroy();

        verify(csServiceInstance).destroyChecker(checkerWithConfig);
    }

    public void testScanWithEmptyFileListReturnsEmptyMap() {
        Map<PsiFile, List<Problem>> result = underTest.scan(Collections.emptyList(), false);

        assertTrue(result.isEmpty());
        verifyNoInteractions(csServiceInstance);
    }

    public void testScanDelegatesToCsServiceInstanceWhenNoScopeIsConfigured() {
        ScannableFile scannableFile = mock(ScannableFile.class);
        List<ScannableFile> files = List.of(scannableFile);
        PsiFile psiFile = myFixture.addFileToProject("Foo.java", "class Foo {}");
        Map<PsiFile, List<Problem>> expected = Map.of(psiFile, List.of());
        when(csServiceInstance.scan(checkerWithConfig, files, false, 4, Optional.empty())).thenReturn(expected);

        Map<PsiFile, List<Problem>> result = underTest.scan(files, false);

        assertSame(expected, result);
    }

    public void testScanPassesSuppressErrorsFlagToCsServiceInstance() {
        ScannableFile scannableFile = mock(ScannableFile.class);
        List<ScannableFile> files = List.of(scannableFile);
        when(csServiceInstance.scan(any(), any(), anyBoolean(), anyInt(), any())).thenReturn(Map.of());

        underTest.scan(files, true);

        verify(csServiceInstance).scan(checkerWithConfig, files, true, 4, Optional.empty());
    }

    public void testScanWithNullPackageSetScopeIncludesAllFiles() {
        // A NamedScope whose PackageSet is null means "no filter" — all files are in scope.
        NamedScope allFilesScope = new NamedScope("TestAll", null);
        CheckStyleChecker scopedChecker = new CheckStyleChecker(
                checkerWithConfig, 4, Optional.empty(), csServiceInstance, Optional.of(allFilesScope));

        PsiFile psiFile = myFixture.addFileToProject("Foo.java", "class Foo {}");
        ScannableFile scannableFile = mock(ScannableFile.class);
        when(scannableFile.getPsiFile()).thenReturn(psiFile);

        Map<PsiFile, List<Problem>> expected = Map.of(psiFile, List.of());
        when(csServiceInstance.scan(any(), any(), anyBoolean(), anyInt(), any())).thenReturn(expected);

        Map<PsiFile, List<Problem>> result = scopedChecker.scan(List.of(scannableFile), false);

        assertSame(expected, result);
        verify(csServiceInstance).scan(checkerWithConfig, List.of(scannableFile), false, 4, Optional.empty());
    }

    public void testScanWithScopeFiltersOutFilesNotInScope() {
        // A PackageSet that never matches any file — all files are excluded.
        NamedScope emptyScope = new NamedScope("TestEmpty", packageSet(file -> false));
        CheckStyleChecker scopedChecker = new CheckStyleChecker(
                checkerWithConfig, 4, Optional.empty(), csServiceInstance, Optional.of(emptyScope));

        PsiFile psiFile = myFixture.addFileToProject("Foo.java", "class Foo {}");
        ScannableFile scannableFile = mock(ScannableFile.class);
        when(scannableFile.getPsiFile()).thenReturn(psiFile);

        Map<PsiFile, List<Problem>> result = scopedChecker.scan(List.of(scannableFile), false);

        assertTrue(result.isEmpty());
        verify(csServiceInstance, never()).scan(any(), any(), anyBoolean(), anyInt(), any());
    }

    public void testScanWithScopePassesOnlyMatchingFilesToCsService() {
        // A PackageSet that matches files whose name contains "Include".
        NamedScope partialScope = new NamedScope("TestPartial",
                packageSet(file -> file.getName().contains("Include")));
        CheckStyleChecker scopedChecker = new CheckStyleChecker(
                checkerWithConfig, 4, Optional.empty(), csServiceInstance, Optional.of(partialScope));

        PsiFile includedFile = myFixture.addFileToProject("IncludeMe.java", "class IncludeMe {}");
        PsiFile excludedFile = myFixture.addFileToProject("ExcludeMe.java", "class ExcludeMe {}");

        ScannableFile includedScannable = mock(ScannableFile.class);
        when(includedScannable.getPsiFile()).thenReturn(includedFile);

        ScannableFile excludedScannable = mock(ScannableFile.class);
        when(excludedScannable.getPsiFile()).thenReturn(excludedFile);

        when(csServiceInstance.scan(any(), any(), anyBoolean(), anyInt(), any())).thenReturn(Map.of());

        scopedChecker.scan(List.of(includedScannable, excludedScannable), false);

        verify(csServiceInstance).scan(
                checkerWithConfig, List.of(includedScannable), false, 4, Optional.empty());
    }

    /**
     * Creates a minimal {@link PackageSet} whose {@code contains()} delegates to {@code predicate}.
     */
    private static PackageSet packageSet(final Predicate<PsiFile> predicate) {
        return new PackageSet() {
            @Override
            public boolean contains(@NotNull final PsiFile file,
                                    @NotNull final NamedScopesHolder holder) {
                return predicate.test(file);
            }

            @Override
            public @NotNull PackageSet createCopy() {
                return this;
            }

            @Override
            public @NotNull String getText() {
                return "";
            }

            @Override
            public int getNodePriority() {
                return 0;
            }
        };
    }
}
