package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;
import org.infernus.idea.checkstyle.util.ProjectPaths;

public class ConventionalConfigurationLocationScannerTest extends BasePlatformTestCase {

    private VirtualFile baseDir() {
        return getProject().getService(ProjectPaths.class).projectPath(getProject());
    }

    public void testFindsConfigCheckstyleCheckstyleXmlWhenPresent() {
        myFixture.addFileToProject("config/checkstyle/checkstyle.xml", "<module/>");

        var found = ConventionalConfigurationLocationScanner.findConventionalConfigurationLocation(
                getProject(), baseDir());

        assertTrue(found.isPresent());
        assertEquals(ConfigurationType.PROJECT_RELATIVE, found.get().getType());
        assertEquals(ConventionalConfigurationLocationScanner.RESERVED_ID, found.get().getId());
        assertEquals(ConventionalConfigurationLocationScanner.RESERVED_DESCRIPTION, found.get().getDescription());
        assertEquals(NamedScopeHelper.getDefaultScope(getProject()), found.get().getNamedScope().orElse(null));
    }

    public void testReturnsEmptyWhenNoConventionalFileExists() {
        var found = ConventionalConfigurationLocationScanner.findConventionalConfigurationLocation(
                getProject(), baseDir());

        assertFalse(found.isPresent());
    }
}
