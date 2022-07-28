package org.infernus.idea.checkstyle.ui;

import com.intellij.psi.search.scope.packageSet.NamedScope;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.util.function.Predicate.not;

/**
 * A table model for editing CheckStyle file locations.
 */
public class LocationTableModel extends AbstractTableModel {
    private static final long serialVersionUID = -7914774770821623832L;

    private static final int COLUMN_ACTIVE = 0;
    private static final int COLUMN_DESCRIPTION = 1;
    private static final int COLUMN_FILE = 2;
    private static final int COLUMN_SCOPE = 3;
    private static final int NUMBER_OF_COLUMNS = 4;

    private final List<ConfigurationLocation> locations = new ArrayList<>();
    private final SortedSet<ConfigurationLocation> activeLocations = new TreeSet<>();

    /**
     * Create a new empty table model.
     */
    public LocationTableModel() {
    }

    public void setLocations(final List<ConfigurationLocation> newLocations) {
        locations.clear();

        if (newLocations != null) {
            locations.addAll(newLocations);
        }

        this.activeLocations.removeIf(not(locations::contains));

        fireTableDataChanged();
    }

    public void addLocation(final ConfigurationLocation location) {
        if (location != null) {
            locations.add(location);
            fireTableRowsInserted(locations.size() - 1, locations.size() - 1);
        }
    }

    public void updateLocation(final ConfigurationLocation location, final ConfigurationLocation newLocation) {
        if (location != null && newLocation != null) {
            final int index = locations.indexOf(location);
            if (index != -1) {
                locations.remove(index);
                locations.add(index, newLocation);
                fireTableRowsUpdated(index, index);
            }
        }
    }

    public void removeLocationAt(final int index) {
        final ConfigurationLocation locationToRemove = locations.get(index);
        if (activeLocations.contains(locationToRemove)) {
            final TreeSet<ConfigurationLocation> newActiveLocations = new TreeSet<>(this.activeLocations);
            newActiveLocations.remove(locationToRemove);
            setActiveLocations(newActiveLocations);
        }
        locations.remove(index);

        fireTableRowsDeleted(index, index);
    }

    public ConfigurationLocation getLocationAt(final int index) {
        return locations.get(index);
    }

    public void setActiveLocations(@NotNull final SortedSet<ConfigurationLocation> activeLocations) {
        if (!activeLocations.isEmpty() && !locations.containsAll(activeLocations)) {
            throw new IllegalArgumentException("Active location is not in location list");
        }

        if (!activeLocations.isEmpty()) {
            activeLocations.forEach(activeLocation -> updateActiveLocation(activeLocation, locations.indexOf(activeLocation), false));
        } else {
            this.activeLocations.clear();
        }
    }

    private void updateActiveLocation(@NotNull final ConfigurationLocation newLocation,
                                      final int newRow,
                                      final boolean allowToggle) {

        if (allowToggle && this.activeLocations.contains(newLocation)) {
            this.activeLocations.remove(newLocation);
        } else {
            this.activeLocations.add(newLocation);
        }

        if (newRow >= 0) {
            fireTableCellUpdated(newRow, COLUMN_ACTIVE);
        }
    }

    public SortedSet<ConfigurationLocation> getActiveLocations() {
        return activeLocations;
    }

    /**
     * Clear all data from this table model.
     */
    public void clear() {
        locations.clear();

        fireTableDataChanged();
    }

    /**
     * Get the properties from the table.
     *
     * @return the map of properties to values.
     */
    public List<ConfigurationLocation> getLocations() {
        return Collections.unmodifiableList(locations);
    }

    @Override
    public int getColumnCount() {
        return NUMBER_OF_COLUMNS;
    }

    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        if (columnIndex == COLUMN_ACTIVE) {
            return Boolean.class;
        } else {
            return String.class;
        }
    }

    @Override
    public String getColumnName(final int column) {
        return CheckStyleBundle.message("config.file.locations.table." + column);
    }

    @Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        return columnIndex == COLUMN_ACTIVE;
    }

    @Override
    public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
        final ConfigurationLocation rowLocation = locations.get(rowIndex);
        if (columnIndex == COLUMN_ACTIVE) {
            updateActiveLocation(rowLocation, rowIndex, true);
        } else {
            throw new IllegalArgumentException("Column is not editable: " + columnIndex);
        }
    }

    @Override
    public int getRowCount() {
        return locations.size();
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        switch (columnIndex) {
            case COLUMN_ACTIVE:
                return activeLocations.contains(locations.get(rowIndex));

            case COLUMN_DESCRIPTION:
                return locations.get(rowIndex).getDescription();

            case COLUMN_FILE:
	            String locationFile = locations.get(rowIndex).getLocation();
	            try {
		            String userInfo = new URL(locationFile).getUserInfo();
                    if (userInfo != null) {
                        return locationFile.replace(
                                userInfo,
                                userInfo.replaceAll("(.*):(.*)", "$1:*****")
                        );
                    }
                    return locationFile;
	            } catch (MalformedURLException e) {
		            return locationFile;
	            }

            case COLUMN_SCOPE:
                return locations.get(rowIndex).getNamedScope()
                        .map(NamedScope::getPresentableName)
                        .orElse("");

            default:
                throw new IllegalArgumentException("Invalid column: "
                        + columnIndex);
        }
    }

}
