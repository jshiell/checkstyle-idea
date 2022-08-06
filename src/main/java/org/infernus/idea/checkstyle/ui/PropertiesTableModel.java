package org.infernus.idea.checkstyle.ui;

import org.infernus.idea.checkstyle.CheckStyleBundle;

import javax.swing.table.AbstractTableModel;
import java.util.*;

/**
 * A table model for editing CheckStyle properties.
 */
public class PropertiesTableModel extends AbstractTableModel {
    private static final long serialVersionUID = -5666606037841678795L;

    private static final int COLUMN_NAME = 0;
    private static final int COLUMN_VALUE = 1;
    private static final int NUMBER_OF_COLUMNS = 2;

    private final List<String> orderedNames = new ArrayList<>();
    private final Map<String, String> properties
            = new HashMap<>();

    /**
     * Create a new empty properties table model.
     */
    public PropertiesTableModel() {
    }

    /**
     * Set the current properties in the table.
     *
     * @param newProperties the map of property names to values.
     */
    public void setProperties(final Map<String, String> newProperties) {
        orderedNames.clear();
        properties.clear();

        if (newProperties != null) {
            properties.putAll(newProperties);
            orderedNames.addAll(newProperties.keySet());
            Collections.sort(orderedNames);
        }

        fireTableDataChanged();
    }

    /**
     * Clear all data from this table model.
     */
    public void clear() {
        orderedNames.clear();
        properties.clear();

        fireTableDataChanged();
    }

    /**
     * Get the properties from the table.
     *
     * @return the map of properties to values.
     */
    public Map<String, String> getProperties() {
        return new HashMap<>(properties);
    }

    @Override
    public int getColumnCount() {
        return NUMBER_OF_COLUMNS;
    }

    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        return String.class;
    }

    @Override
    public String getColumnName(final int column) {
        return CheckStyleBundle.message("config.file.properties.table." + column);
    }

    @Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        return columnIndex == COLUMN_VALUE;
    }

    @Override
    public void setValueAt(final Object aValue, final int rowIndex,
                           final int columnIndex) {
        if (columnIndex == COLUMN_VALUE) {
            final String propertyName = orderedNames.get(rowIndex);
            properties.put(propertyName, aValue != null
                    ? aValue.toString() : null);
        } else {
                throw new IllegalArgumentException("Invalid column: "
                        + columnIndex);
        }
    }

    @Override
    public int getRowCount() {
        return orderedNames.size();
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        switch (columnIndex) {
            case COLUMN_NAME:
                return orderedNames.get(rowIndex);
            case COLUMN_VALUE:
                return properties.get(orderedNames.get(rowIndex));

            default:
                throw new IllegalArgumentException("Invalid column: "
                        + columnIndex);
        }
    }
}
