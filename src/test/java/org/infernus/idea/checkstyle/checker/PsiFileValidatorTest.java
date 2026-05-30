package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.config.PluginConfiguration;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PsiFileValidator's early-guard conditions.
 *
 * <p>Tests that require the full IntelliJ platform (isInSource, isGenerated, isTestClass,
 * hasDocument) cannot be covered without BasePlatformTestCase infrastructure, because they
 * depend on static service lookups (PsiDocumentManager, ProjectFileIndex, ModuleUtil,
 * JavaProjectRootsUtil) that require a running application.
 *
 * <p>Regression note: the isInNamedScopeIfPresent empty-stream bug (returning false instead of
 * true when no named scopes are present) is exercised indirectly via isScannable(); full coverage
 * of that path requires a platform test fixture.
 */
@ExtendWith(MockitoExtension.class)
class PsiFileValidatorTest {

    @Mock
    private Module module;

    @Mock
    private PsiFile psiFile;

    private final PluginConfiguration config = PluginConfigurationBuilder.testInstance("10.0.0").build();

    @Test
    void nullPsiFileIsNotScannable() {
        assertFalse(PsiFileValidator.isScannable(null, module, config, null));
    }

    @Test
    void invalidPsiFileIsNotScannable() {
        when(psiFile.isValid()).thenReturn(false);
        assertFalse(PsiFileValidator.isScannable(psiFile, module, config, null));
    }

    @Test
    void nonPhysicalPsiFileIsNotScannable() {
        when(psiFile.isValid()).thenReturn(true);
        when(psiFile.isPhysical()).thenReturn(false);
        assertFalse(PsiFileValidator.isScannable(psiFile, module, config, null));
    }
}
