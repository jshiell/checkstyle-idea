package org.infernus.idea.checkstyle.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStyleConstants;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class PropertiesPanel extends JPanel {
    private static final Log LOG = LogFactory.getLog(PropertiesPanel.class);

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

    private ConfigurationLocation configurationLocation;

    public PropertiesPanel(final Project project) {
        super(new BorderLayout());

        if (project == null) {
            throw new IllegalArgumentException("Project may not be null");
        }
        this.project = project;

        initialise();
    }

    private void initialise() {
        setBorder(new EmptyBorder(8, 8, 4, 8));
        setPreferredSize(new Dimension(500, 400));

        final ResourceBundle resources = ResourceBundle.getBundle(CheckStyleConstants.RESOURCE_BUNDLE);
        propertiesTable.setToolTipText(resources.getString("config.file.properties.tooltip"));
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
        InputStream configInputStream = null;
        try {
            configInputStream = configurationLocation.resolve();
            propertiesModel.setProperties(configurationLocation.getProperties());

        } catch (IOException e) {
            LOG.error("Couldn't resolve properties file", e);

            final ResourceBundle resources = ResourceBundle.getBundle(
                    CheckStyleConstants.RESOURCE_BUNDLE);

            final String message = resources.getString("config.file.resolve-failed");
            final String formattedMessage = new MessageFormat(message).format(new Object[]{e.getMessage()});
            Messages.showErrorDialog(project, formattedMessage,
                    resources.getString("config.file.error.title"));

        } finally {
            if (configInputStream != null) {
                try {
                    configInputStream.close();
                } catch (IOException e) {
                    // ignored
                }
            }
        }
    }
}
