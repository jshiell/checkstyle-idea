package org.infernus.idea.checkstyle.ui;

import org.infernus.idea.checkstyle.CheckStyleConstants;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

/**
 * A table model for editing CheckStyle file locations.
 *
 * @author James Shiell
 * @version 1.0
 */
public class LocationTableModel extends AbstractTableModel {

    protected static final int COLUMN_ACTIVE = 0;
    protected static final int COLUMN_DESCRIPTION = 1;
    protected static final int COLUMN_FILE = 2;

    private final List<ConfigurationLocation> locations = new ArrayList<ConfigurationLocation>();
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

        if (locations.size() > 0 && (activeLocation == null || !locations.contains(activeLocation))) {
            activeLocation = locations.get(0);
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

    public void removeLocation(final ConfigurationLocation location) {
        if (location != null) {
            final int index = locations.indexOf(location);
            if (index != -1) {
                locations.remove(index);
                fireTableRowsDeleted(index, index);
            }
        }
    }

    public void removeLocationAt(final int index) {
        locations.remove(index);

        fireTableRowsDeleted(index, index);
    }

    public ConfigurationLocation getLocationAt(final int index) {
        return locations.get(index);
    }

    public void setActiveLocation(final ConfigurationLocation activeLocation) {
        if (activeLocation != null && !locations.contains(activeLocation)) {
            throw new IllegalArgumentException("Active location is not in location list");
        }

        updateActiveLocation(activeLocation, locations.indexOf(activeLocation));
    }

    private void updateActiveLocation(final ConfigurationLocation activeLocation, final int rowIndex) {
        int oldColumn = -1;
        for (int i = 0; i < locations.size(); ++i) {
            if (i != rowIndex && equals(locations.get(i), this.activeLocation)) {
                oldColumn = i;
                break;
            }
        }

        this.activeLocation = activeLocation;

        fireTableCellUpdated(rowIndex, COLUMN_ACTIVE);

        if (oldColumn != -1) {
            fireTableCellUpdated(oldColumn, COLUMN_ACTIVE);
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

    /**
     * {@inheritDoc}
     */
    public int getColumnCount() {
        return 3;
    }

    /**
     * {@inheritDoc}
     */
    public Class<?> getColumnClass(final int columnIndex) {
        switch (columnIndex) {
            case COLUMN_ACTIVE:
                return Boolean.class;

            default:
                return String.class;
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getColumnName(final int column) {
        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);

        return resources.getString("config.file.locations.table." + column);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        return columnIndex == COLUMN_ACTIVE;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
        final ConfigurationLocation rowLocation = locations.get(rowIndex);

        switch (columnIndex) {
            case COLUMN_ACTIVE:
                updateActiveLocation(rowLocation, rowIndex);
                break;

            default:
                throw new IllegalArgumentException("Column is not editable: " + columnIndex);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getRowCount() {
        return locations.size();
    }

    /**
     * {@inheritDoc}
     */
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        switch (columnIndex) {
            case COLUMN_ACTIVE:
                return equals(locations.get(rowIndex), activeLocation);

            case COLUMN_DESCRIPTION:
                return locations.get(rowIndex).getDescription();

            case COLUMN_FILE:
                return locations.get(rowIndex).getLocation();

            default:
                throw new IllegalArgumentException("Invalid column: "
                        + columnIndex);
        }
    }
}
