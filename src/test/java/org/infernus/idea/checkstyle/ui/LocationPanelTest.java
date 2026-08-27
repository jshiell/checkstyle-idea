package org.infernus.idea.checkstyle.ui;

import com.intellij.testFramework.LightPlatformTestCase;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.infernus.idea.checkstyle.model.BundledConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;

public class LocationPanelTest extends LightPlatformTestCase {

    private LocationPanel panel;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        panel = new LocationPanel(getProject());
    }

    public void testSelectingBuiltInGoogleChecksWithCustomDescriptionYieldsABundledConfigurationLocation() {
        panel.builtInLocationRadio().doClick();
        panel.builtInComboBox().setSelectedItem(BundledConfig.GOOGLE_CHECKS);
        panel.descriptionField().setText("My Custom Google Checks");

        final ConfigurationLocation location = panel.getConfigurationLocation();

        assertTrue("expected a BundledConfigurationLocation", location instanceof BundledConfigurationLocation);
        final BundledConfigurationLocation bundled = (BundledConfigurationLocation) location;
        assertEquals(BundledConfig.GOOGLE_CHECKS, bundled.getBundledConfig());
        assertEquals("My Custom Google Checks", bundled.getDescription());
    }

    public void testSelectingBuiltInRadioWithoutTouchingComboPrefillsDescription() {
        panel.builtInLocationRadio().doClick();

        assertEquals(((BundledConfig) panel.builtInComboBox().getSelectedItem()).getDescription(),
                panel.descriptionField().getText());
    }

    public void testSetConfigurationLocationRoundTripsABundledLocationIntoComboAndDescription() {
        final ConfigurationLocationFactory factory = getProject().getService(ConfigurationLocationFactory.class);
        final BundledConfigurationLocation location = factory.create(BundledConfig.GOOGLE_CHECKS, getProject());
        location.setDescription("Existing Custom Description");

        panel.setConfigurationLocation(location);

        assertTrue("built-in radio should be selected", panel.builtInLocationRadio().isSelected());
        assertEquals(BundledConfig.GOOGLE_CHECKS, panel.builtInComboBox().getSelectedItem());
        assertEquals("Existing Custom Description", panel.descriptionField().getText());
    }
}
