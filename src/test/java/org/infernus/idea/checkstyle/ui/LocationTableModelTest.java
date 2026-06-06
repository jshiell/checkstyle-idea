package org.infernus.idea.checkstyle.ui;

import com.intellij.testFramework.LightPlatformTestCase;
import org.infernus.idea.checkstyle.StringConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;

import java.util.List;
import java.util.TreeSet;

public class LocationTableModelTest extends LightPlatformTestCase {

    private LocationTableModel model;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        model = new LocationTableModel();
    }

    public void testUpdateLocationPreservesActiveStatus() {
        final StringConfigurationLocation original = new StringConfigurationLocation("<xml/>", getProject());
        original.setDescription("Original");

        model.setLocations(List.of(original));

        final TreeSet<ConfigurationLocation> active = new TreeSet<>();
        active.add(original);
        model.setActiveLocations(active);

        assertTrue(model.getActiveLocations().contains(original));

        final StringConfigurationLocation updated = new StringConfigurationLocation("<xml/>", getProject());
        updated.setDescription("Updated");

        model.updateLocation(original, updated);

        assertTrue("active status should survive a description change", model.getActiveLocations().contains(updated));
    }
}
