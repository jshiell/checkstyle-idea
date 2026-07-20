package org.infernus.idea.checkstyle.ui;

import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.config.ApplicationConfigurationState.GlobalConfigurationLocation;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.AbstractTableModel;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Table model for the IDE-wide global rule location list shown in {@link CheckStyleApplicationConfigurable}.
 * Operates on {@link GlobalConfigurationLocation} DTOs directly (no project context required).
 */
public class GlobalLocationTableModel extends AbstractTableModel {

    @Serial
    private static final long serialVersionUID = -6231891475110872941L;

    private static final int COLUMN_ACTIVE = 0;
    private static final int COLUMN_DESCRIPTION = 1;
    private static final int COLUMN_TYPE = 2;
    private static final int COLUMN_LOCATION = 3;
    private static final int NUMBER_OF_COLUMNS = 4;

    private final List<GlobalConfigurationLocation> locations = new ArrayList<>();
    /** IDs of locations whose "Active" checkbox is checked. Insertion-ordered for stable round-trips. */
    private final Set<String> activeIds = new LinkedHashSet<>();

    public void setLocations(@NotNull final List<GlobalConfigurationLocation> newLocations,
                             @NotNull final List<String> newActiveIds) {
        locations.clear();
        locations.addAll(newLocations);
        activeIds.clear();
        activeIds.addAll(newActiveIds);
        fireTableDataChanged();
    }

    public void addLocation(@NotNull final GlobalConfigurationLocation location) {
        locations.add(location);
        fireTableRowsInserted(locations.size() - 1, locations.size() - 1);
    }

    public void updateLocationAt(final int index, @NotNull final GlobalConfigurationLocation updated) {
        final GlobalConfigurationLocation old = locations.get(index);
        final boolean wasActive = activeIds.remove(old.id);
        locations.set(index, updated);
        if (wasActive) {
            activeIds.add(updated.id);
        }
        fireTableRowsUpdated(index, index);
    }

    public void removeLocationAt(final int index) {
        final GlobalConfigurationLocation removed = locations.remove(index);
        activeIds.remove(removed.id);
        fireTableRowsDeleted(index, index);
    }

    @NotNull
    public GlobalConfigurationLocation getLocationAt(final int index) {
        return locations.get(index);
    }

    @NotNull
    public List<GlobalConfigurationLocation> getLocations() {
        return Collections.unmodifiableList(locations);
    }

    @NotNull
    public List<String> getActiveIds() {
        return new ArrayList<>(activeIds);
    }

    @Override
    public int getColumnCount() {
        return NUMBER_OF_COLUMNS;
    }

    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        if (columnIndex == COLUMN_ACTIVE) {
            return Boolean.class;
        }
        return String.class;
    }

    @Override
    public String getColumnName(final int column) {
        return switch (column) {
            case COLUMN_ACTIVE -> CheckStyleBundle.message("config.file.locations.table.0");
            case COLUMN_DESCRIPTION -> CheckStyleBundle.message("config.file.locations.table.1");
            case COLUMN_TYPE -> CheckStyleBundle.message("config.global.locations.table.type");
            case COLUMN_LOCATION -> CheckStyleBundle.message("config.file.locations.table.2");
            default -> throw new IllegalArgumentException("Invalid column: " + column);
        };
    }

    @Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        return columnIndex == COLUMN_ACTIVE;
    }

    @Override
    public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
        if (columnIndex == COLUMN_ACTIVE) {
            final String id = locations.get(rowIndex).id;
            if (activeIds.contains(id)) {
                activeIds.remove(id);
            } else {
                activeIds.add(id);
            }
            fireTableCellUpdated(rowIndex, COLUMN_ACTIVE);
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
        final GlobalConfigurationLocation location = locations.get(rowIndex);
        return switch (columnIndex) {
            case COLUMN_ACTIVE -> activeIds.contains(location.id);
            case COLUMN_DESCRIPTION -> Objects.requireNonNullElse(location.description, "");
            case COLUMN_TYPE -> Objects.requireNonNullElse(location.type, "");
            case COLUMN_LOCATION -> Objects.requireNonNullElse(location.location, "");
            default -> throw new IllegalArgumentException("Invalid column: " + columnIndex);
        };
    }
}
