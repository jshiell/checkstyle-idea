package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;
import org.infernus.idea.checkstyle.util.ProjectFilePaths;
import org.infernus.idea.checkstyle.util.ProjectPaths;

public class ConventionalConfigurationLocationScannerTest extends BasePlatformTestCase {

    private PluginConfigurationManager configManager() {
        return getProject().getService(PluginConfigurationManager.class);
    }

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

    public void testPrefersConfigCheckstyleCheckstyleXmlOverRootCheckstyleXmlWhenBothExist() {
        myFixture.addFileToProject("config/checkstyle/checkstyle.xml", "<module/>");
        myFixture.addFileToProject("checkstyle.xml", "<module/>");
        var expected = baseDir().findFileByRelativePath("config/checkstyle/checkstyle.xml");

        var found = ConventionalConfigurationLocationScanner.findConventionalConfigurationLocation(
                getProject(), baseDir());

        assertTrue(found.isPresent());
        assertEquals(expected.getPath(), new ProjectFilePaths(getProject()).detokenise(found.get().getLocation()));
    }

    public void testFallsBackToRootCheckstyleXmlWhenOnlyThatExists() {
        myFixture.addFileToProject("checkstyle.xml", "<module/>");
        var expected = baseDir().findFileByRelativePath("checkstyle.xml");

        var found = ConventionalConfigurationLocationScanner.findConventionalConfigurationLocation(
                getProject(), baseDir());

        assertTrue(found.isPresent());
        assertEquals(expected.getPath(), new ProjectFilePaths(getProject()).detokenise(found.get().getLocation()));
    }

    public void testFallsBackToEtcCheckstyleXmlWhenOnlyThatExists() {
        myFixture.addFileToProject("etc/checkstyle.xml", "<module/>");
        var expected = baseDir().findFileByRelativePath("etc/checkstyle.xml");

        var found = ConventionalConfigurationLocationScanner.findConventionalConfigurationLocation(
                getProject(), baseDir());

        assertTrue(found.isPresent());
        assertEquals(expected.getPath(), new ProjectFilePaths(getProject()).detokenise(found.get().getLocation()));
    }

    public void testSkipsAPathThatIsADirectoryRatherThanAFile() {
        myFixture.addFileToProject("checkstyle.xml/placeholder.txt", "");
        myFixture.addFileToProject("etc/checkstyle.xml", "<module/>");
        var expected = baseDir().findFileByRelativePath("etc/checkstyle.xml");

        var found = ConventionalConfigurationLocationScanner.findConventionalConfigurationLocation(
                getProject(), baseDir());

        assertTrue(found.isPresent());
        assertEquals(expected.getPath(), new ProjectFilePaths(getProject()).detokenise(found.get().getLocation()));
    }

    public void testAddsDetectedLocationWhenNoneExistedPreviously() {
        myFixture.addFileToProject("config/checkstyle/checkstyle.xml", "<module/>");

        var outcome = ConventionalConfigurationLocationScanner.rescan(getProject());

        assertEquals(ConventionalConfigurationLocationScanner.ScanOutcome.ADDED, outcome);
        var current = configManager().getCurrent();
        assertTrue(current.getLocations().stream()
                .anyMatch(loc -> ConventionalConfigurationLocationScanner.RESERVED_ID.equals(loc.getId())));
        assertTrue(current.getActiveLocationIds().contains(ConventionalConfigurationLocationScanner.RESERVED_ID));
    }

    public void testRescanningWithTheSameFilePresentDoesNotDuplicateTheLocation() {
        myFixture.addFileToProject("config/checkstyle/checkstyle.xml", "<module/>");
        ConventionalConfigurationLocationScanner.rescan(getProject());

        var outcome = ConventionalConfigurationLocationScanner.rescan(getProject());

        assertEquals(ConventionalConfigurationLocationScanner.ScanOutcome.UNCHANGED_PRESENT, outcome);
        var reservedLocations = configManager().getCurrent().getLocations().stream()
                .filter(loc -> ConventionalConfigurationLocationScanner.RESERVED_ID.equals(loc.getId()))
                .toList();
        assertEquals(1, reservedLocations.size());
    }

    public void testRemovesPreviouslyDetectedLocationWhenTheFileIsDeleted() throws Exception {
        var file = myFixture.addFileToProject("config/checkstyle/checkstyle.xml", "<module/>").getVirtualFile();
        ConventionalConfigurationLocationScanner.rescan(getProject());

        WriteAction.runAndWait(() -> file.delete(this));

        var outcome = ConventionalConfigurationLocationScanner.rescan(getProject());

        assertEquals(ConventionalConfigurationLocationScanner.ScanOutcome.REMOVED, outcome);
        var current = configManager().getCurrent();
        assertTrue(current.getLocations().stream()
                .noneMatch(loc -> ConventionalConfigurationLocationScanner.RESERVED_ID.equals(loc.getId())));
        assertFalse(current.getActiveLocationIds().contains(ConventionalConfigurationLocationScanner.RESERVED_ID));
    }
}
