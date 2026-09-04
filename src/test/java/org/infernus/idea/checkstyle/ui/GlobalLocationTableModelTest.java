package org.infernus.idea.checkstyle.ui;

import org.infernus.idea.checkstyle.config.ApplicationConfigurationState.GlobalConfigurationLocation;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalLocationTableModelTest {

    @Test
    void setLocationsReplacesRowsAndActiveIds() {
        GlobalLocationTableModel model = new GlobalLocationTableModel();
        GlobalConfigurationLocation first = new GlobalConfigurationLocation("id-1", "LOCAL_FILE", "c:/a.xml", "A", "All");

        model.setLocations(List.of(first), List.of("id-1"));

        assertEquals(1, model.getRowCount());
        assertEquals(List.of("id-1"), model.getActiveIds());
        assertEquals(true, model.getValueAt(0, 0));
        assertEquals("A", model.getValueAt(0, 1));
        assertEquals("LOCAL_FILE", model.getValueAt(0, 2));
        assertEquals("c:/a.xml", model.getValueAt(0, 3));
        assertEquals("All", model.getValueAt(0, 4));
    }

    @Test
    void getValueAtFallsBackToDefaultScopeWhenScopeMissing() {
        GlobalLocationTableModel model = new GlobalLocationTableModel();
        GlobalConfigurationLocation location = new GlobalConfigurationLocation("id-1", "LOCAL_FILE", "c:/a.xml", "A", null);

        model.setLocations(List.of(location), List.of());

        assertEquals(NamedScopeHelper.DEFAULT_SCOPE_ID, model.getValueAt(0, 4));
    }

    @Test
    void setValueAtTogglesActiveState() {
        GlobalLocationTableModel model = new GlobalLocationTableModel();
        GlobalConfigurationLocation first = new GlobalConfigurationLocation("id-1", "LOCAL_FILE", "c:/a.xml", "A", "All");
        model.setLocations(List.of(first), List.of());

        model.setValueAt(true, 0, 0);
        assertTrue((Boolean) model.getValueAt(0, 0));
        assertEquals(List.of("id-1"), model.getActiveIds());

        model.setValueAt(false, 0, 0);
        assertFalse((Boolean) model.getValueAt(0, 0));
        assertEquals(List.of(), model.getActiveIds());
    }

    @Test
    void updateLocationAtMovesActiveIdToUpdatedLocationId() {
        GlobalLocationTableModel model = new GlobalLocationTableModel();
        GlobalConfigurationLocation original = new GlobalConfigurationLocation("id-1", "LOCAL_FILE", "c:/a.xml", "A", "All");
        GlobalConfigurationLocation updated = new GlobalConfigurationLocation("id-2", "LOCAL_FILE", "c:/b.xml", "B", "All");
        model.setLocations(List.of(original), List.of("id-1"));

        model.updateLocationAt(0, updated);

        assertEquals(List.of("id-2"), model.getActiveIds());
        assertEquals("id-2", model.getLocationAt(0).id);
    }

    @Test
    void removeLocationAtAlsoRemovesActiveId() {
        GlobalLocationTableModel model = new GlobalLocationTableModel();
        GlobalConfigurationLocation first = new GlobalConfigurationLocation("id-1", "LOCAL_FILE", "c:/a.xml", "A", "All");
        model.setLocations(List.of(first), List.of("id-1"));

        model.removeLocationAt(0);

        assertEquals(0, model.getRowCount());
        assertEquals(List.of(), model.getActiveIds());
    }

    @Test
    void nonActiveColumnCannotBeEditedViaSetValueAt() {
        GlobalLocationTableModel model = new GlobalLocationTableModel();
        GlobalConfigurationLocation first = new GlobalConfigurationLocation("id-1", "LOCAL_FILE", "c:/a.xml", "A", "All");
        model.setLocations(List.of(first), List.of());

        assertThrows(IllegalArgumentException.class, () -> model.setValueAt("x", 0, 1));
    }
}
