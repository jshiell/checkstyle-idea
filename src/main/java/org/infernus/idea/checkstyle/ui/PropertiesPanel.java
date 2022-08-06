package org.infernus.idea.checkstyle.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.infernus.idea.checkstyle.CheckStyleBundle;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

public class PropertiesPanel extends JPanel {
    private static final Logger LOG = Logger.getInstance(PropertiesPanel.class);

    private final PropertiesTableModel propertiesModel = new PropertiesTableModel();

    /**
     * Properties table, hacked for enable/disable support.
     */
    private final JBTable propertiesTable = new JBTable(propertiesModel) {
        @NotNull
        public Component prepareRenderer(@NotNull final TableCellRenderer renderer,
                                         final int row, final int column) {
            final Component comp = super.prepareRenderer(renderer, row, column);
            comp.setEnabled(isEnabled());
            return comp;
        }
    };

    private final Project project;
    private final CheckstyleProjectService checkstyleProjectService;

    private ConfigurationLocation configurationLocation;

    public PropertiesPanel(@NotNull final Project project,
                           @NotNull final CheckstyleProjectService checkstyleProjectService) {
        super(new BorderLayout());

        this.project = project;
        this.checkstyleProjectService = checkstyleProjectService;

        initialise();
    }

    private void initialise() {
        setBorder(JBUI.Borders.empty(8, 8, 4, 8));
        setPreferredSize(new Dimension(500, 400));

        propertiesTable.setToolTipText(CheckStyleBundle.message("config.file.properties.tooltip"));
        propertiesTable.setStriped(true);
        propertiesTable.getTableHeader().setReorderingAllowed(false);

        final JScrollPane propertiesScrollPane = new JBScrollPane(propertiesTable,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        add(propertiesScrollPane, BorderLayout.CENTER);
    }

    /**
     * Get the configuration location entered in the dialogue, or null if no valid location was entered.
     *
     * @return the location or null if no valid location entered.
     */
    public ConfigurationLocation getConfigurationLocation() {
        commitCellEdits();

        configurationLocation.setProperties(propertiesModel.getProperties());
        return configurationLocation;
    }

    private void commitCellEdits() {
        final TableCellEditor cellEditor = propertiesTable.getCellEditor();
        if (cellEditor != null) {
            cellEditor.stopCellEditing();
        }
    }

    /**
     * Set the configuration location.
     *
     * @param configurationLocation the location.
     */
    public void setConfigurationLocation(final ConfigurationLocation configurationLocation) {
        this.configurationLocation = (ConfigurationLocation) configurationLocation.clone();

        // get latest properties from file
        try (InputStream configInputStream = configurationLocation.resolve(checkstyleProjectService.underlyingClassLoader())) {
            propertiesModel.setProperties(configurationLocation.getProperties());

        } catch (IOException e) {
            LOG.warn("Couldn't resolve properties file", e);

            Messages.showErrorDialog(project, CheckStyleBundle.message("config.file.resolve-failed", e.getMessage()),
                    CheckStyleBundle.message("config.file.error.title"));

        }
    }
}
