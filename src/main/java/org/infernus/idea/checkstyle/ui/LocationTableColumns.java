package org.infernus.idea.checkstyle.ui;

import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.TableColumn;

/**
 * Column sizing shared by the project and global configuration location tables.
 */
public final class LocationTableColumns {

    private static final int ACTIVE_COLUMN = 0;
    private static final int DESCRIPTION_COLUMN = 1;
    private static final int SCOPE_COLUMN = 3;

    private static final int ACTIVE_MIN_WIDTH = 40;
    private static final int ACTIVE_MAX_WIDTH = 50;
    private static final int DESCRIPTION_MIN_WIDTH = 120;
    private static final int DESCRIPTION_PREFERRED_WIDTH = 180;
    private static final int DESCRIPTION_MAX_WIDTH = 240;
    private static final int SCOPE_MIN_WIDTH = 60;
    private static final int SCOPE_PREFERRED_WIDTH = 100;
    private static final int SCOPE_MAX_WIDTH = 160;

    public static void apply(@NotNull final JBTable table) {
        final TableColumn activeColumn = table.getColumnModel().getColumn(ACTIVE_COLUMN);
        activeColumn.setMinWidth(ACTIVE_MIN_WIDTH);
        activeColumn.setPreferredWidth(ACTIVE_MAX_WIDTH);
        activeColumn.setMaxWidth(ACTIVE_MAX_WIDTH);
        activeColumn.setResizable(false);

        setWidths(table.getColumnModel().getColumn(DESCRIPTION_COLUMN),
                DESCRIPTION_MIN_WIDTH, DESCRIPTION_PREFERRED_WIDTH, DESCRIPTION_MAX_WIDTH);
        setWidths(table.getColumnModel().getColumn(SCOPE_COLUMN),
                SCOPE_MIN_WIDTH, SCOPE_PREFERRED_WIDTH, SCOPE_MAX_WIDTH);
    }

    private static void setWidths(final TableColumn column, final int min, final int preferred, final int max) {
        column.setMinWidth(min);
        column.setPreferredWidth(preferred);
        column.setMaxWidth(max);
    }

    private LocationTableColumns() {
    }
}
