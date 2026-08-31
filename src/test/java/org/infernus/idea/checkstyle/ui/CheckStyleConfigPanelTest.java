package org.infernus.idea.checkstyle.ui;

import com.intellij.testFramework.LightPlatformTestCase;
import org.infernus.idea.checkstyle.ThirdPartyJarCache;
import org.infernus.idea.checkstyle.config.ConventionalConfigurationLocationScanner.ScanOutcome;
import org.infernus.idea.checkstyle.config.ConventionalConfigurationLocationScanner.ScanResult;
import org.infernus.idea.checkstyle.config.PluginConfiguration;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.infernus.idea.checkstyle.model.BundledConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class CheckStyleConfigPanelTest extends LightPlatformTestCase {

    private PluginConfigurationManager configurationManager;
    private CheckStyleConfigPanel panel;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        configurationManager = getProject().getService(PluginConfigurationManager.class);
        configurationManager.setCurrent(PluginConfigurationBuilder.defaultConfiguration(getProject()).build(), false);
        panel = new CheckStyleConfigPanel(getProject());
    }

    public void testAnUntouchedCheckboxReturnsTheLiveScanBeforeCheckin() {
        panel.showPluginConfiguration(setScanBeforeCheckin(true));

        assertTrue("an untouched checkbox should return the live flag",
                panel.getPluginConfiguration().isScanBeforeCheckin());
    }

    public void testAnUntouchedCheckboxPicksUpAnEditMadeAfterThePanelWasShown() {
        panel.showPluginConfiguration(setScanBeforeCheckin(false));

        // the Commit settings page writes while our panel is open
        setScanBeforeCheckin(true);

        assertTrue("an untouched checkbox should not overwrite a later edit",
                panel.getPluginConfiguration().isScanBeforeCheckin());
    }

    public void testATickedCheckboxIsWrittenBack() {
        panel.showPluginConfiguration(setScanBeforeCheckin(false));

        panel.getScanBeforeCheckinCheckbox().setSelected(true);

        assertTrue("a checkbox the user ticked should be written back",
                panel.getPluginConfiguration().isScanBeforeCheckin());
    }

    public void testImportSettingsFromGradleCheckboxRoundTrips() {
        panel.showPluginConfiguration(PluginConfigurationBuilder.from(configurationManager.getCurrent())
                .withImportSettingsFromGradle(false)
                .build());

        panel.getImportSettingsFromGradleCheckbox().setSelected(true);

        assertTrue("a ticked checkbox should be written back",
                panel.getPluginConfiguration().isImportSettingsFromGradle());
    }

    public void testScrollToSourceIsCarriedThrough() {
        final PluginConfiguration configuration = PluginConfigurationBuilder
                .from(configurationManager.getCurrent())
                .withScrollToSource(true)
                .build();
        configurationManager.setCurrent(configuration, false);
        panel.showPluginConfiguration(configuration);

        assertTrue("a flag the panel has no widget for should not be reset",
                panel.getPluginConfiguration().isScrollToSource());
    }

    public void testEditAndRemoveAreEnabledForAUserCreatedBundledCopyButNotForTheCanonicalSeededRow() {
        final ConfigurationLocationFactory factory = getProject().getService(ConfigurationLocationFactory.class);
        final BundledConfigurationLocation canonical = factory.create(BundledConfig.GOOGLE_CHECKS, getProject());
        final ConfigurationLocation userCopy = factory.create(getProject(), "a-user-created-copy-id", ConfigurationType.BUNDLED,
                BundledConfig.GOOGLE_CHECKS.getId(), "My Custom Google Checks", NamedScopeHelper.getDefaultScope(getProject()));

        panel.showPluginConfiguration(PluginConfigurationBuilder.from(configurationManager.getCurrent())
                .withLocations(locationsOf(canonical, userCopy))
                .build());

        final int canonicalIndex = panel.locationModel().getLocations().indexOf(canonical);
        final int userCopyIndex = panel.locationModel().getLocations().indexOf(userCopy);

        assertFalse("the canonical seeded row should not be editable/removable",
                panel.isEditOrRemoveEnabledFor(canonicalIndex));
        assertTrue("a user-created copy should be editable/removable",
                panel.isEditOrRemoveEnabledFor(userCopyIndex));
    }

    public void testTwoDifferentlyDescribedBundledCopiesSurviveTheAddDuplicateCheckAndTheApplyDedup() {
        final ConfigurationLocationFactory factory = getProject().getService(ConfigurationLocationFactory.class);
        final BundledConfigurationLocation canonical = factory.create(BundledConfig.GOOGLE_CHECKS, getProject());
        final ConfigurationLocation userCopy = factory.create(getProject(), "a-user-created-copy-id", ConfigurationType.BUNDLED,
                BundledConfig.GOOGLE_CHECKS.getId(), "My Custom Google Checks", NamedScopeHelper.getDefaultScope(getProject()));

        panel.showPluginConfiguration(PluginConfigurationBuilder.from(configurationManager.getCurrent())
                .withLocations(locationsOf(canonical))
                .build());

        // mirrors AddLocationAction's duplicate check
        assertFalse("a differently-described copy should not be flagged as a duplicate of the canonical entry",
                panel.locationModel().getLocations().contains(userCopy));

        panel.locationModel().addLocation(userCopy);

        // mirrors the Apply path's dedup via new TreeSet<>(locationModel.getLocations())
        final Set<ConfigurationLocation> applied = panel.getPluginConfiguration().getLocations();
        assertEquals(2, applied.size());
        assertTrue(applied.contains(canonical));
        assertTrue(applied.contains(userCopy));
    }

    public void testAddingASecondBundledCopyWithAnUneditedDuplicateDescriptionIsRejected() {
        final ConfigurationLocationFactory factory = getProject().getService(ConfigurationLocationFactory.class);
        final BundledConfigurationLocation canonical = factory.create(BundledConfig.GOOGLE_CHECKS, getProject());

        panel.showPluginConfiguration(PluginConfigurationBuilder.from(configurationManager.getCurrent())
                .withLocations(locationsOf(canonical))
                .build());

        final ConfigurationLocation secondCopyWithUneditedDescription = factory.create(getProject(), "another-copy-id",
                ConfigurationType.BUNDLED, BundledConfig.GOOGLE_CHECKS.getId(), BundledConfig.GOOGLE_CHECKS.getDescription(),
                NamedScopeHelper.getDefaultScope(getProject()));

        assertTrue("a second bundled copy with an unedited (prefilled) description matching an existing "
                        + "bundled entry should be flagged as a duplicate",
                panel.hasDuplicateBundledDescription(secondCopyWithUneditedDescription, null));
    }

    public void testEditingANonBundledLocationToShareAnotherNonBundledLocationsDescriptionIsNotRejected() {
        final ConfigurationLocationFactory factory = getProject().getService(ConfigurationLocationFactory.class);
        final ConfigurationLocation fileA = factory.create(getProject(), "file-a-id", ConfigurationType.LOCAL_FILE,
                "/path/to/a.xml", "My Rules", NamedScopeHelper.getDefaultScope(getProject()));
        final ConfigurationLocation fileB = factory.create(getProject(), "file-b-id", ConfigurationType.LOCAL_FILE,
                "/path/to/b.xml", "Some Other Rules", NamedScopeHelper.getDefaultScope(getProject()));

        panel.showPluginConfiguration(PluginConfigurationBuilder.from(configurationManager.getCurrent())
                .withLocations(locationsOf(fileA, fileB))
                .build());

        final ConfigurationLocation editedFileB = factory.create(getProject(), "file-b-id", ConfigurationType.LOCAL_FILE,
                "/path/to/b.xml", "My Rules", NamedScopeHelper.getDefaultScope(getProject()));

        assertFalse("two non-bundled locations have always been allowed to share a description",
                panel.wouldCollideOnEdit(fileB, editedFileB));
    }

    public void testRenamingACopyToCollideWithAnotherEntryIsRejectedAsADuplicate() {
        final ConfigurationLocationFactory factory = getProject().getService(ConfigurationLocationFactory.class);
        final BundledConfigurationLocation canonical = factory.create(BundledConfig.GOOGLE_CHECKS, getProject());
        final ConfigurationLocation userCopy = factory.create(getProject(), "a-user-created-copy-id", ConfigurationType.BUNDLED,
                BundledConfig.GOOGLE_CHECKS.getId(), "My Custom Google Checks", NamedScopeHelper.getDefaultScope(getProject()));

        panel.showPluginConfiguration(PluginConfigurationBuilder.from(configurationManager.getCurrent())
                .withLocations(locationsOf(canonical, userCopy))
                .build());

        final ConfigurationLocation renamedBackToCanonical = factory.create(getProject(), userCopy.getId(), ConfigurationType.BUNDLED,
                BundledConfig.GOOGLE_CHECKS.getId(), BundledConfig.GOOGLE_CHECKS.getDescription(), NamedScopeHelper.getDefaultScope(getProject()));
        final ConfigurationLocation renamedToSomethingStillUnique = factory.create(getProject(), userCopy.getId(), ConfigurationType.BUNDLED,
                BundledConfig.GOOGLE_CHECKS.getId(), "Another Unique Description", NamedScopeHelper.getDefaultScope(getProject()));

        assertTrue("renaming a copy back to the canonical description should collide",
                panel.wouldCollideOnEdit(userCopy, renamedBackToCanonical));
        assertFalse("renaming a copy to a still-unique description should not collide",
                panel.wouldCollideOnEdit(userCopy, renamedToSomethingStillUnique));
    }

    public void testApplyDetectionResultForAddedAppendsAndActivatesTheRow() {
        final ConfigurationLocationFactory factory = getProject().getService(ConfigurationLocationFactory.class);
        final ConfigurationLocation newLocation = factory.create(getProject(), "conventional-config-location",
                ConfigurationType.PROJECT_RELATIVE, "config/checkstyle/checkstyle.xml", "Detected Checkstyle Configuration",
                NamedScopeHelper.getDefaultScope(getProject()));

        panel.applyDetectionResult(new ScanResult(ScanOutcome.ADDED, List.of(newLocation), Optional.of(newLocation)));

        assertTrue(panel.locationModel().getLocations().contains(newLocation));
        assertTrue(panel.locationModel().getActiveLocations().contains(newLocation));
    }

    public void testApplyDetectionResultForRemovedDropsTheRowAndDeactivatesIt() {
        final ConfigurationLocation reservedRow = reservedRow();
        panel.showPluginConfiguration(PluginConfigurationBuilder.from(configurationManager.getCurrent())
                .withLocations(locationsOf(reservedRow))
                .withActiveLocationIds(new TreeSet<>(List.of(reservedRow.getId())))
                .build());

        panel.applyDetectionResult(new ScanResult(ScanOutcome.REMOVED, List.of(), Optional.empty()));

        assertFalse(panel.locationModel().getLocations().contains(reservedRow));
        assertFalse(panel.locationModel().getActiveLocations().contains(reservedRow));
    }

    public void testApplyDetectionResultForReplacedLeavesExactlyOneReservedRowActiveAtTheNewLocation() {
        final ConfigurationLocationFactory factory = getProject().getService(ConfigurationLocationFactory.class);
        final ConfigurationLocation oldReservedRow = factory.create(getProject(), "conventional-config-location",
                ConfigurationType.PROJECT_RELATIVE, "checkstyle.xml", "Detected Checkstyle Configuration",
                NamedScopeHelper.getDefaultScope(getProject()));
        panel.showPluginConfiguration(PluginConfigurationBuilder.from(configurationManager.getCurrent())
                .withLocations(locationsOf(oldReservedRow))
                .withActiveLocationIds(new TreeSet<>(List.of(oldReservedRow.getId())))
                .build());
        final ConfigurationLocation newReservedRow = factory.create(getProject(), "conventional-config-location",
                ConfigurationType.PROJECT_RELATIVE, "config/checkstyle/checkstyle.xml", "Detected Checkstyle Configuration",
                NamedScopeHelper.getDefaultScope(getProject()));

        panel.applyDetectionResult(new ScanResult(ScanOutcome.REPLACED, List.of(newReservedRow), Optional.of(newReservedRow)));

        assertFalse(panel.locationModel().getLocations().contains(oldReservedRow));
        assertFalse(panel.locationModel().getActiveLocations().contains(oldReservedRow));
        assertEquals(List.of(newReservedRow), panel.locationModel().getLocations());
        assertTrue(panel.locationModel().getActiveLocations().contains(newReservedRow));
    }

    public void testApplyDetectionResultForNoProjectDirectoryIsANoOp() {
        final ConfigurationLocation existing = reservedRow();
        panel.showPluginConfiguration(PluginConfigurationBuilder.from(configurationManager.getCurrent())
                .withLocations(locationsOf(existing))
                .withActiveLocationIds(new TreeSet<>(List.of(existing.getId())))
                .build());
        final List<ConfigurationLocation> locationsBefore = panel.locationModel().getLocations();
        final SortedSet<ConfigurationLocation> activeBefore = new TreeSet<>(panel.locationModel().getActiveLocations());

        panel.applyDetectionResult(new ScanResult(ScanOutcome.NO_PROJECT_DIRECTORY, locationsBefore, Optional.empty()));

        assertEquals(locationsBefore, panel.locationModel().getLocations());
        assertEquals(activeBefore, panel.locationModel().getActiveLocations());
    }

    public void testApplyDetectionResultForAddedDoesNotTouchThePersistedConfiguration() {
        final ConfigurationLocationFactory factory = getProject().getService(ConfigurationLocationFactory.class);
        final ConfigurationLocation newLocation = factory.create(getProject(), "conventional-config-location",
                ConfigurationType.PROJECT_RELATIVE, "config/checkstyle/checkstyle.xml", "Detected Checkstyle Configuration",
                NamedScopeHelper.getDefaultScope(getProject()));
        final PluginConfiguration before = configurationManager.getCurrent();

        panel.applyDetectionResult(new ScanResult(ScanOutcome.ADDED, List.of(newLocation), Optional.of(newLocation)));

        assertTrue(panel.getPluginConfiguration().getLocations().contains(newLocation));
        assertTrue(panel.getPluginConfiguration().getActiveLocationIds().contains(newLocation.getId()));
        assertEquals(before, configurationManager.getCurrent());
    }

    public void testApplyingTheSameDetectionResultTwiceDoesNotDuplicateTheRow() {
        final ConfigurationLocationFactory factory = getProject().getService(ConfigurationLocationFactory.class);
        final ConfigurationLocation newLocation = factory.create(getProject(), "conventional-config-location",
                ConfigurationType.PROJECT_RELATIVE, "config/checkstyle/checkstyle.xml", "Detected Checkstyle Configuration",
                NamedScopeHelper.getDefaultScope(getProject()));
        panel.applyDetectionResult(new ScanResult(ScanOutcome.ADDED, List.of(newLocation), Optional.of(newLocation)));

        // Mirrors what scan() would actually return on a second call: the row is already in the
        // snapshot, so the outcome is UNCHANGED_PRESENT and the location list is unchanged. getLocations()
        // returns a live view over the model's backing list, so it must be snapshotted (as scan() itself
        // always does) before being fed back into setLocations(), or the model's own clear() wipes it.
        final List<ConfigurationLocation> secondCallLocations = List.copyOf(panel.locationModel().getLocations());
        panel.applyDetectionResult(new ScanResult(ScanOutcome.UNCHANGED_PRESENT, secondCallLocations, Optional.of(newLocation)));

        assertEquals(1, panel.locationModel().getLocations().stream().filter(newLocation::equals).count());
    }

    public void testApplyDetectionResultForUnchangedPresentReactivatesAManuallyDeactivatedRow() {
        final ConfigurationLocation reservedRow = reservedRow();
        panel.showPluginConfiguration(PluginConfigurationBuilder.from(configurationManager.getCurrent())
                .withLocations(locationsOf(reservedRow))
                .withActiveLocationIds(new TreeSet<>(List.of(reservedRow.getId())))
                .build());
        panel.locationModel().setActiveLocations(new TreeSet<>());
        assertFalse(panel.locationModel().getActiveLocations().contains(reservedRow));

        panel.applyDetectionResult(new ScanResult(ScanOutcome.UNCHANGED_PRESENT, List.of(reservedRow), Optional.of(reservedRow)));

        assertTrue(panel.locationModel().getActiveLocations().contains(reservedRow));
    }

    public void testApplyUrlClasspathEntryAddsTheRawUrlToTheThirdPartyClasspath() {
        panel.setThirdPartyJarCacheForTesting(new ThirdPartyJarCache(java.nio.file.Path.of("unused"),
                (url, target) -> { throw new AssertionError("no network access expected"); }));

        panel.applyUrlClasspathEntry("https://example.invalid/custom-check.jar");

        assertTrue(panel.getPluginConfiguration().getThirdPartyClasspath()
                .contains("https://example.invalid/custom-check.jar"));
    }

    public void testApplyUrlClasspathEntryCalledTwiceWithTheSameUrlDoesNotAddADuplicate() {
        panel.setThirdPartyJarCacheForTesting(new ThirdPartyJarCache(java.nio.file.Path.of("unused"),
                (url, target) -> { throw new AssertionError("no network access expected"); }));

        panel.applyUrlClasspathEntry("https://example.invalid/custom-check.jar");
        panel.applyUrlClasspathEntry("https://example.invalid/custom-check.jar");

        assertEquals(1, panel.getPluginConfiguration().getThirdPartyClasspath().stream()
                .filter("https://example.invalid/custom-check.jar"::equals)
                .count());
    }

    public void testNormalizeCapturedUrlTrimsSurroundingWhitespace() {
        // runAddUrl()/runEditUrl() apply this to text captured from the Add/Edit dialogs before
        // it reaches the duplicate check, the download helper, or the classpath list, so that
        // "https://example.invalid/custom-check.jar" and the same URL with surrounding whitespace
        // are treated as one entry rather than downloading and caching separately.
        assertEquals("https://example.invalid/custom-check.jar",
                CheckStyleConfigPanel.normalizeCapturedUrl("  https://example.invalid/custom-check.jar  "));
    }

    private ConfigurationLocation reservedRow() {
        final ConfigurationLocationFactory factory = getProject().getService(ConfigurationLocationFactory.class);
        return factory.create(getProject(), "conventional-config-location",
                ConfigurationType.PROJECT_RELATIVE, "config/checkstyle/checkstyle.xml", "Detected Checkstyle Configuration",
                NamedScopeHelper.getDefaultScope(getProject()));
    }

    private SortedSet<ConfigurationLocation> locationsOf(final ConfigurationLocation... locations) {
        final SortedSet<ConfigurationLocation> result = new TreeSet<>();
        result.addAll(java.util.Arrays.asList(locations));
        return result;
    }

    private PluginConfiguration setScanBeforeCheckin(final boolean scanBeforeCheckin) {
        final PluginConfiguration configuration = PluginConfigurationBuilder
                .from(configurationManager.getCurrent())
                .withScanBeforeCheckin(scanBeforeCheckin)
                .build();
        configurationManager.setCurrent(configuration, false);
        return configuration;
    }
}
