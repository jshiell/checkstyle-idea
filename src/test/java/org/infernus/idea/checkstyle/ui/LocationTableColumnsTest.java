package org.infernus.idea.checkstyle.ui;

import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.ui.table.JBTable;

import javax.swing.JTable;
import javax.swing.table.TableColumn;

public class LocationTableColumnsTest extends LightPlatformTestCase {

    private JBTable table;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        table = new JBTable(new LocationTableModel());
        LocationTableColumns.apply(table);
    }

    public void testActiveColumnIsFixedAndNotResizable() {
        final TableColumn column = table.getColumnModel().getColumn(0);

        assertColumn(column, 40, 50, 50);
        assertFalse(column.getResizable());
    }

    public void testDescriptionColumnWidths() {
        assertColumn(table.getColumnModel().getColumn(1), 120, 180, 240);
    }

    public void testScopeColumnWidths() {
        assertColumn(table.getColumnModel().getColumn(3), 60, 100, 160);
    }

    public void testLocationColumnTakesRemainingSpace() {
        final TableColumn column = table.getColumnModel().getColumn(2);

        assertEquals(Integer.MAX_VALUE, column.getMaxWidth());
        assertTrue(column.getResizable());
    }

    public void testUsesDefaultAutoResizeMode() {
        assertEquals(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS, table.getAutoResizeMode());
    }

    private static void assertColumn(final TableColumn column, final int min, final int preferred, final int max) {
        assertEquals(min, column.getMinWidth());
        assertEquals(preferred, column.getPreferredWidth());
        assertEquals(max, column.getMaxWidth());
    }
}
