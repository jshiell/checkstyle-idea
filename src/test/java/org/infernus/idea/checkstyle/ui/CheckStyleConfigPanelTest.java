package org.infernus.idea.checkstyle.ui;

import com.intellij.testFramework.LightPlatformTestCase;
import org.infernus.idea.checkstyle.config.PluginConfiguration;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.infernus.idea.checkstyle.model.BundledConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;

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
