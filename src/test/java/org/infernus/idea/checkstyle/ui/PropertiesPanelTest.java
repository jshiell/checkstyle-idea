package org.infernus.idea.checkstyle.ui;

import com.intellij.testFramework.LightPlatformTestCase;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.VersionListReader;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.infernus.idea.checkstyle.model.BundledConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;

import java.util.Map;

/**
 * Characterises the Edit round-trip that increment 2's clone() fix targets: PropertiesPanel clones the
 * location it is given so the dialogue can be cancelled without side effects, then applies the edited
 * properties back onto that clone. A user-created bundled copy's id, named scope, and properties must
 * all survive that clone -> edit -> apply path intact and independent of the original.
 */
public class PropertiesPanelTest extends LightPlatformTestCase {

    private static final String BUNDLED_VERSION = new VersionListReader().getBundledVersions().last();

    private static final Map<String, String> CUSTOM_PROPERTIES = Map.of(
            "org.checkstyle.google.suppressionfilter.config", "my-suppressions.xml",
            "org.checkstyle.google.suppressionxpathfilter.config", "my-xpath-suppressions.xml");

    private PropertiesPanel panel;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        final CheckstyleProjectService checkstyleProjectService = getProject().getService(CheckstyleProjectService.class);
        checkstyleProjectService.activateCheckstyleVersion(BUNDLED_VERSION, null);

        panel = new PropertiesPanel(getProject(), checkstyleProjectService);
    }

    public void testEditingAUserCreatedBundledCopyPreservesItsIdScopeAndPropertiesIndependentlyOfTheOriginal() {
        final ConfigurationLocationFactory factory = getProject().getService(ConfigurationLocationFactory.class);
        final BundledConfigurationLocation original = (BundledConfigurationLocation) factory.create(getProject(),
                "a-user-created-copy-id", ConfigurationType.BUNDLED, BundledConfig.GOOGLE_CHECKS.getId(),
                "My Custom Google Checks", NamedScopeHelper.getDefaultScope(getProject()));
        original.setProperties(CUSTOM_PROPERTIES);

        panel.setConfigurationLocation(original);

        // mutate the original after handing it to the panel: the edited result must not see this,
        // proving setConfigurationLocation() cloned rather than aliased it.
        original.setProperties(Map.of(
                "org.checkstyle.google.suppressionfilter.config", "mutated-after-clone.xml",
                "org.checkstyle.google.suppressionxpathfilter.config", "mutated-after-clone.xml"));

        final ConfigurationLocation edited = panel.getConfigurationLocation();

        assertNotSame(original, edited);
        assertTrue("edited location should still be a BundledConfigurationLocation", edited instanceof BundledConfigurationLocation);
        assertEquals(original.getId(), edited.getId());
        assertEquals(BundledConfig.GOOGLE_CHECKS, ((BundledConfigurationLocation) edited).getBundledConfig());
        // resolving google_checks.xml also discovers other ${}-declared properties (e.g. severity) and
        // fills them in with their file default, so assert on our two explicitly-set values rather than
        // the whole map.
        CUSTOM_PROPERTIES.forEach((property, value) -> assertEquals(value, edited.getProperties().get(property)));
        assertEquals(original.getNamedScope(), edited.getNamedScope());
    }
}
