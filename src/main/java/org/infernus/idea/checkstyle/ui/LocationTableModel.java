package org.infernus.idea.checkstyle.ui;

import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.Nullable;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private ConfigurationLocation activeLocation;

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

        if (activeLocation != null && !locations.contains(activeLocation)) {
            activeLocation = null;
        }

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
        if (equals(locations.get(index), activeLocation)) {
            setActiveLocation(null);
        }
        locations.remove(index);

        fireTableRowsDeleted(index, index);
    }

    public ConfigurationLocation getLocationAt(final int index) {
        return locations.get(index);
    }

    public void setActiveLocation(@Nullable final ConfigurationLocation activeLocation) {
        if (activeLocation != null && !locations.contains(activeLocation)) {
            throw new IllegalArgumentException("Active location is not in location list");
        }

        if (activeLocation != null) {
            updateActiveLocation(activeLocation, locations.indexOf(activeLocation), false);
        } else {
            updateActiveLocation(null, -1, false);
        }
    }

    private void updateActiveLocation(@Nullable final ConfigurationLocation newLocation,
                                      final int newRow,
                                      final boolean allowToggle) {
        int oldRow = -1;
        for (int currentRow = 0; currentRow < locations.size(); ++currentRow) {
            if (equals(locations.get(currentRow), this.activeLocation)) {
                oldRow = currentRow;
                break;
            }
        }

        if (oldRow == newRow && allowToggle) {
            this.activeLocation = null;
        } else {
            this.activeLocation = newLocation;
        }

        if (newRow >= 0) {
            fireTableCellUpdated(newRow, COLUMN_ACTIVE);
        }
        if (oldRow >= 0) {
            fireTableCellUpdated(oldRow, COLUMN_ACTIVE);
        }
    }

    /*
     * This is a port from commons-lang 2.4, in order to get around the absence of commons-lang in
     * some packages of IDEA 7.x.
     */
    private boolean equals(final Object object1, final Object object2) {
        if (object1 == object2) {
            return true;
        }
        if ((object1 == null) || (object2 == null)) {
            return false;
        }
        return object1.equals(object2);
    }

    public ConfigurationLocation getActiveLocation() {
        return activeLocation;
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
                return equals(locations.get(rowIndex), activeLocation);

            case COLUMN_DESCRIPTION:
                return locations.get(rowIndex).getDescription();

            case COLUMN_FILE:
                return locations.get(rowIndex).getLocation();

            case COLUMN_SCOPE:
                return locations.get(rowIndex).getScope().getName();

            default:
                throw new IllegalArgumentException("Invalid column: "
                        + columnIndex);
        }
    }

}
