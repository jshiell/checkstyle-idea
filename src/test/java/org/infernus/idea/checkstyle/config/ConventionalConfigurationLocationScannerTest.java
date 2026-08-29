package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;
import org.infernus.idea.checkstyle.util.ProjectFilePaths;
import org.infernus.idea.checkstyle.util.ProjectPaths;

public class ConventionalConfigurationLocationScannerTest extends BasePlatformTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // The light project fixture is reused across test methods, so PluginConfigurationManager's
        // state (including anything a previous test's rescan() added) otherwise leaks between tests.
        configManager().setCurrent(PluginConfigurationBuilder.defaultConfiguration(getProject()).build(), false);
    }

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

    public void testSwitchesToAHigherPriorityLocationWhenItAppears() {
        myFixture.addFileToProject("checkstyle.xml", "<module/>");
        ConventionalConfigurationLocationScanner.rescan(getProject());
        myFixture.addFileToProject("config/checkstyle/checkstyle.xml", "<module/>");
        var expected = baseDir().findFileByRelativePath("config/checkstyle/checkstyle.xml");

        var outcome = ConventionalConfigurationLocationScanner.rescan(getProject());

        assertEquals(ConventionalConfigurationLocationScanner.ScanOutcome.REPLACED, outcome);
        var reservedLocations = configManager().getCurrent().getLocations().stream()
                .filter(loc -> ConventionalConfigurationLocationScanner.RESERVED_ID.equals(loc.getId()))
                .toList();
        assertEquals(1, reservedLocations.size());
        assertEquals(expected.getPath(),
                new ProjectFilePaths(getProject()).detokenise(reservedLocations.get(0).getLocation()));
    }

    public void testDoesNotTouchManuallyAddedLocationsOrActiveIds() {
        myFixture.addFileToProject("config/checkstyle/checkstyle.xml", "<module/>");
        var factory = getProject().getService(ConfigurationLocationFactory.class);
        var manualLocation = factory.create(getProject(), "manual-location", ConfigurationType.PROJECT_RELATIVE,
                "some-other.xml", "Manually Added", NamedScopeHelper.getDefaultScope(getProject()));
        configManager().setCurrent(
                PluginConfigurationBuilder.from(configManager().getCurrent())
                        .withLocations(new TreeSet<>(List.of(manualLocation)))
                        .withActiveLocationIds(new TreeSet<>(List.of(manualLocation.getId())))
                        .build(),
                true);
        var locationCountBeforeRescan = configManager().getCurrent().getLocations().size();

        var outcome = ConventionalConfigurationLocationScanner.rescan(getProject());

        assertEquals(ConventionalConfigurationLocationScanner.ScanOutcome.ADDED, outcome);
        var current = configManager().getCurrent();
        assertTrue(current.getLocations().contains(manualLocation));
        assertTrue(current.getActiveLocationIds().contains("manual-location"));
        assertTrue(current.getLocations().stream()
                .anyMatch(loc -> ConventionalConfigurationLocationScanner.RESERVED_ID.equals(loc.getId())));
        assertEquals(locationCountBeforeRescan + 1, current.getLocations().size());
    }

    public void testScanWithEmptySnapshotAddsTheDetectedLocation() {
        myFixture.addFileToProject("config/checkstyle/checkstyle.xml", "<module/>");

        var result = ConventionalConfigurationLocationScanner.scan(getProject(), List.of());

        assertEquals(ConventionalConfigurationLocationScanner.ScanOutcome.ADDED, result.outcome());
        assertTrue(result.found().isPresent());
        assertTrue(result.locations().contains(result.found().get()));
    }

    public void testScanWithReservedRowAlreadyPresentAndFileStillPresentReturnsUnchangedPresent() {
        myFixture.addFileToProject("config/checkstyle/checkstyle.xml", "<module/>");
        var reservedRow = ConventionalConfigurationLocationScanner.findConventionalConfigurationLocation(
                getProject(), baseDir()).orElseThrow();

        var result = ConventionalConfigurationLocationScanner.scan(getProject(), List.of(reservedRow));

        assertEquals(ConventionalConfigurationLocationScanner.ScanOutcome.UNCHANGED_PRESENT, result.outcome());
        var reservedRows = result.locations().stream()
                .filter(loc -> ConventionalConfigurationLocationScanner.RESERVED_ID.equals(loc.getId()))
                .toList();
        assertEquals(1, reservedRows.size());
    }

    public void testScanWithReservedRowPresentAndFileDeletedReturnsRemoved() throws Exception {
        var file = myFixture.addFileToProject("config/checkstyle/checkstyle.xml", "<module/>").getVirtualFile();
        var reservedRow = ConventionalConfigurationLocationScanner.findConventionalConfigurationLocation(
                getProject(), baseDir()).orElseThrow();
        WriteAction.runAndWait(() -> file.delete(this));

        var result = ConventionalConfigurationLocationScanner.scan(getProject(), List.of(reservedRow));

        assertEquals(ConventionalConfigurationLocationScanner.ScanOutcome.REMOVED, result.outcome());
        assertTrue(result.locations().stream()
                .noneMatch(loc -> ConventionalConfigurationLocationScanner.RESERVED_ID.equals(loc.getId())));
    }

    public void testScanWithRootLevelReservedRowAndHigherPriorityFileReturnsReplaced() {
        myFixture.addFileToProject("checkstyle.xml", "<module/>");
        var rootReservedRow = ConventionalConfigurationLocationScanner.findConventionalConfigurationLocation(
                getProject(), baseDir()).orElseThrow();
        myFixture.addFileToProject("config/checkstyle/checkstyle.xml", "<module/>");
        var expected = baseDir().findFileByRelativePath("config/checkstyle/checkstyle.xml");

        var result = ConventionalConfigurationLocationScanner.scan(getProject(), List.of(rootReservedRow));

        assertEquals(ConventionalConfigurationLocationScanner.ScanOutcome.REPLACED, result.outcome());
        var reservedRows = result.locations().stream()
                .filter(loc -> ConventionalConfigurationLocationScanner.RESERVED_ID.equals(loc.getId()))
                .toList();
        assertEquals(1, reservedRows.size());
        assertEquals(expected.getPath(),
                new ProjectFilePaths(getProject()).detokenise(reservedRows.get(0).getLocation()));
    }

    public void testScanNeverTouchesPluginConfigurationManager() {
        var callCount = new AtomicInteger();
        ConfigurationListener listener = callCount::incrementAndGet;
        configManager().addConfigurationListener(listener);
        var before = configManager().getCurrent();
        myFixture.addFileToProject("config/checkstyle/checkstyle.xml", "<module/>");

        ConventionalConfigurationLocationScanner.scan(getProject(), List.of());
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertEquals(0, callCount.get());
        assertEquals(before, configManager().getCurrent());
    }

    public void testScanDoesNotDuplicateAnAlreadyPresentEquivalentLocation() {
        myFixture.addFileToProject("config/checkstyle/checkstyle.xml", "<module/>");
        var reservedRow = ConventionalConfigurationLocationScanner.findConventionalConfigurationLocation(
                getProject(), baseDir()).orElseThrow();

        var result = ConventionalConfigurationLocationScanner.scan(getProject(), List.of(reservedRow));

        var matches = result.locations().stream().filter(reservedRow::equals).toList();
        assertEquals(1, matches.size());
    }

    public void testScanPreservesNonReservedLocationsAndTheirOrder() {
        var factory = getProject().getService(ConfigurationLocationFactory.class);
        var first = factory.create(getProject(), "first-id", ConfigurationType.PROJECT_RELATIVE,
                "first.xml", "First", NamedScopeHelper.getDefaultScope(getProject()));
        var second = factory.create(getProject(), "second-id", ConfigurationType.PROJECT_RELATIVE,
                "second.xml", "Second", NamedScopeHelper.getDefaultScope(getProject()));

        var result = ConventionalConfigurationLocationScanner.scan(getProject(), List.of(first, second));

        assertEquals(List.of(first, second), result.locations());
    }

    public void testMakesNoChangeAndFiresNoListenerWhenNothingIsDetectedAndNothingExistedBefore() {
        var callCount = new AtomicInteger();
        ConfigurationListener listener = callCount::incrementAndGet;
        configManager().addConfigurationListener(listener);

        var outcome = ConventionalConfigurationLocationScanner.rescan(getProject());
        // configManager.setCurrent() (if reached) fires listeners via invokeLater, so the queued
        // event must be pumped before the assertion can see whether it ran.
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertEquals(ConventionalConfigurationLocationScanner.ScanOutcome.UNCHANGED_ABSENT, outcome);
        assertEquals(0, callCount.get());
    }
}
