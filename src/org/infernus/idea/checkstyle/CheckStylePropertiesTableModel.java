package org.infernus.idea.checkstyle;

import javax.swing.table.AbstractTableModel;
import java.util.*;

/**
 * A table model for editing CheckStyle properties.
 *
 * @author James Shiell
 * @version 1.0
 */
public class CheckStylePropertiesTableModel extends AbstractTableModel {

    protected static final int COLUMN_NAME = 0;
    protected static final int COLUMN_VALUE = 1;

    private final List<String> orderedNames = new ArrayList<String>();
    private final Map<String, String> properties
            = new HashMap<String, String>();

    /**
     * Create a new empty properties table model.
     */
    public CheckStylePropertiesTableModel() {
    }

    /**
     * Create a new properties table model.
     *
     * @param properties the map of property names to values.
     */
    public CheckStylePropertiesTableModel(
            final Map<String, String> properties) {
        setProperties(properties);
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
        return new HashMap<String, String>(properties);
    }

    /**
     * {@inheritDoc}
     */
    public int getColumnCount() {
        return 2;
    }

    /**
     * {@inheritDoc}
     */
    public Class<?> getColumnClass(final int columnIndex) {
        return String.class;
    }

    /**
     * {@inheritDoc}
     */
    public String getColumnName(final int column) {
        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);

        return resources.getString("config.file.properties.table." + column);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        return columnIndex == COLUMN_VALUE;
    }

    /**
     * {@inheritDoc}
     */
    public void setValueAt(final Object aValue, final int rowIndex,
                           final int columnIndex) {
        switch (columnIndex) {
            case COLUMN_VALUE:
                final String propertyName = orderedNames.get(rowIndex);
                properties.put(propertyName, aValue != null
                        ? aValue.toString() : null);
                break;

            default:
                throw new IllegalArgumentException("Invalid column: "
                        + columnIndex);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getRowCount() {
        return orderedNames.size();
    }

    /**
     * {@inheritDoc}
     */
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
