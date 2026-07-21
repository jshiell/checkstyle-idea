package org.infernus.idea.checkstyle;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.infernus.idea.checkstyle.checker.CheckerFactoryCache;
import org.infernus.idea.checkstyle.config.ApplicationConfigurationState;
import org.infernus.idea.checkstyle.config.ApplicationConfigurationState.GlobalConfigurationLocation;
import org.infernus.idea.checkstyle.config.ArtifactRepositoryCredentialsStore;
import org.infernus.idea.checkstyle.config.PasswordSafeArtifactRepositoryCredentialsStore;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.ui.GlobalLocationDialogue;
import org.infernus.idea.checkstyle.ui.GlobalLocationTableModel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.HashSet;
import java.util.Objects;


/**
 * The application-level (IDE-wide) "configurable component" for CheckStyle plugin settings that are not
 * scoped to a single project. Registered in {@code plugin.xml} as an {@code applicationConfigurable} extension.
 */
public class CheckStyleApplicationConfigurable implements Configurable {

    private static final Dimension DECORATOR_DIMENSIONS = new Dimension(300, 150);
    public static final int ACTIVE_COL_MIN_WIDTH = 40;
    public static final int ACTIVE_COL_MAX_WIDTH = 50;
    public static final int LOCATION_INPUT_MIN_WIDTH = 120;
    public static final int LOCATION_INPUT_PREFERRED_WIDTH = 180;
    public static final int LOCATION_INPUT_MAX_WIDTH = 240;
    public static final int TYPE_INPUT_MIN_WDITH = 80;
    public static final int TYPE_INPUT_PREFERRED_WIDTH = 120;
    public static final int TYPE_INPUT_MAX_WIDTH = 160;

    private final ApplicationConfigurationState applicationConfigurationState;
    private final ArtifactRepositoryCredentialsStore credentialsStore;

    private JTextField artifactRepositoryBaseUrlOverrideField;
    private JTextField artifactRepositoryOverrideUsernameField;
    private JPasswordField artifactRepositoryOverridePasswordField;

    private JCheckBox useGlobalRulesByDefaultCheckbox;

    private final GlobalLocationTableModel globalLocationTableModel = new GlobalLocationTableModel();

    private JBTable globalLocationTable;

    public CheckStyleApplicationConfigurable() {
        this(ApplicationManager.getApplication().getService(ApplicationConfigurationState.class),
                new PasswordSafeArtifactRepositoryCredentialsStore());
    }

    CheckStyleApplicationConfigurable(@NotNull final ApplicationConfigurationState applicationConfigurationState,
                                      @NotNull final ArtifactRepositoryCredentialsStore credentialsStore) {
        this.applicationConfigurationState = applicationConfigurationState;
        this.credentialsStore = credentialsStore;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return CheckStyleBundle.message("config.application.configuration-name");
    }

    @Override
    public JComponent createComponent() {
        artifactRepositoryBaseUrlOverrideField = new JTextField();
        artifactRepositoryBaseUrlOverrideField.setToolTipText(
                CheckStyleBundle.message("config.artefact-repository-base-url-override.tooltip"));

        artifactRepositoryOverrideUsernameField = new JTextField();
        artifactRepositoryOverrideUsernameField.setToolTipText(
                CheckStyleBundle.message("config.artefact-repository-override-username.tooltip"));

        artifactRepositoryOverridePasswordField = new JPasswordField();
        artifactRepositoryOverridePasswordField.setToolTipText(
                CheckStyleBundle.message("config.artefact-repository-override-password.tooltip"));

        useGlobalRulesByDefaultCheckbox = new JCheckBox(
                CheckStyleBundle.message("config.global.use-by-default.text"));
        useGlobalRulesByDefaultCheckbox.setToolTipText(
                CheckStyleBundle.message("config.global.use-by-default.tooltip"));

        globalLocationTable = new JBTable(globalLocationTableModel);
        globalLocationTable.setStriped(true);
        globalLocationTable.getTableHeader().setReorderingAllowed(false);

        setColumnWidth(globalLocationTable, 1, LOCATION_INPUT_MIN_WIDTH, LOCATION_INPUT_PREFERRED_WIDTH, LOCATION_INPUT_MAX_WIDTH);
        setColumnWidth(globalLocationTable, 2, TYPE_INPUT_MIN_WDITH, TYPE_INPUT_PREFERRED_WIDTH, TYPE_INPUT_MAX_WIDTH);

        final var activeColumn = globalLocationTable.getColumnModel().getColumn(0);
        activeColumn.setMinWidth(ACTIVE_COL_MIN_WIDTH);
        activeColumn.setPreferredWidth(ACTIVE_COL_MAX_WIDTH);
        activeColumn.setMaxWidth(ACTIVE_COL_MAX_WIDTH);
        activeColumn.setResizable(false);

        reset();

        return createForm();
    }

    private JPanel createForm() {
        final JComponent globalRulesEditor = createGlobalRulesEditor();
        return FormBuilder.createFormBuilder()
                .addComponent(getDescriptionTextArea())
                .addLabeledComponent(
                        CheckStyleBundle.message("config.artefact-repository-base-url-override.label.text"),
                        artifactRepositoryBaseUrlOverrideField)
                .addLabeledComponent(
                        CheckStyleBundle.message("config.artefact-repository-override-username.label.text"),
                        artifactRepositoryOverrideUsernameField)
                .addLabeledComponent(
                        CheckStyleBundle.message("config.artefact-repository-override-password.label.text"),
                        artifactRepositoryOverridePasswordField)
                .addComponent(new TitledSeparator(CheckStyleBundle.message("config.global.section.title")))
                .addComponent(useGlobalRulesByDefaultCheckbox)
                .addLabeledComponent(
                        CheckStyleBundle.message("config.global.locations.label"),
                        globalRulesEditor,
                        JBUI.scale(4),
                        true)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    private @NotNull JTextArea getDescriptionTextArea() {
        final JTextArea description = new JTextArea(CheckStyleBundle.message("config.artefact-repository-base-url-override.description"));
        description.setFont(UIManager.getFont("Label.font"));
        description.setEditable(false);
        description.setOpaque(false);
        description.setWrapStyleWord(true);
        description.setLineWrap(true);
        return description;
    }

    private static void setColumnWidth(JTable table, int index, int min, int preferred, int max) {
        final TableColumn column = table.getColumnModel().getColumn(index);
        column.setMinWidth(min);
        column.setPreferredWidth(preferred);
        column.setMaxWidth(max);
    }

    @Override
    public boolean isModified() {
        if (!Objects.equals(
                normalise(artifactRepositoryBaseUrlOverrideField.getText()),
                normalise(applicationConfigurationState.getArtifactRepositoryBaseUrlOverride()))) {
            return true;
        }

<<        String typedUsername = emptyIfNull(normalise(artifactRepositoryOverrideUsernameField.getText()));
        String persistedUsername = emptyIfNull(applicationConfigurationState.getArtifactRepositoryOverrideUsername());
        if (!typedUsername.equals(persistedUsername)) {
            return true;
        }

        String typedPassword = new String(artifactRepositoryOverridePasswordField.getPassword());
        String storedPassword = persistedUsername.isBlank()
                ? "" : credentialsStore.getPassword(persistedUsername).orElse("");
        if (!typedPassword.equals(storedPassword)) {
            return true;
        }

        if (useGlobalRulesByDefaultCheckbox.isSelected() != applicationConfigurationState.isUseGlobalRulesByDefault()
                || !globalLocationTableModel.getLocations().equals(applicationConfigurationState.getGlobalLocations())
                || !new HashSet<>(globalLocationTableModel.getActiveIds()).equals(
                        new HashSet<>(applicationConfigurationState.getActiveGlobalLocationIds()))) {
            return true;
        }

        return false;\n    }\n
    @Override
    public void apply() {
        String previousUsername = emptyIfNull(applicationConfigurationState.getArtifactRepositoryOverrideUsername());
        String newUsername = emptyIfNull(normalise(artifactRepositoryOverrideUsernameField.getText()));
        String typedPassword = new String(artifactRepositoryOverridePasswordField.getPassword());

        applicationConfigurationState.setArtifactRepositoryBaseUrlOverride(
                normalise(artifactRepositoryBaseUrlOverrideField.getText()));
        applicationConfigurationState.setArtifactRepositoryOverrideUsername(
                newUsername.isBlank() ? null : newUsername);

        if (!newUsername.equals(previousUsername) && !previousUsername.isBlank()) {
            credentialsStore.setPassword(previousUsername, "");
        }
        if (!newUsername.isBlank()) {
            credentialsStore.setPassword(newUsername, typedPassword);
        }

        applicationConfigurationState.setUseGlobalRulesByDefault(useGlobalRulesByDefaultCheckbox.isSelected());
        applicationConfigurationState.setGlobalLocations(globalLocationTableModel.getLocations());
        applicationConfigurationState.setActiveGlobalLocationIds(globalLocationTableModel.getActiveIds());

        // Invalidate checker caches in all open projects so stale global-location checkers are evicted.\n        final ProjectManager projectManager = ProjectManager.getInstanceIfCreated();
        if (projectManager != null) {
            for (final Project project : projectManager.getOpenProjects()) {
                project.getService(CheckerFactoryCache.class).invalidate();
                project.getService(PluginConfigurationManager.class).invalidate();
            }
        }
    }

    @Override
    public void reset() {
        artifactRepositoryBaseUrlOverrideField.setText(
                Objects.requireNonNullElse(applicationConfigurationState.getArtifactRepositoryBaseUrlOverride(), ""));

        String persistedUsername = emptyIfNull(applicationConfigurationState.getArtifactRepositoryOverrideUsername());
        artifactRepositoryOverrideUsernameField.setText(persistedUsername);
        artifactRepositoryOverridePasswordField.setText(
                persistedUsername.isBlank() ? "" : credentialsStore.getPassword(persistedUsername).orElse(""));

        useGlobalRulesByDefaultCheckbox.setSelected(applicationConfigurationState.isUseGlobalRulesByDefault());
        globalLocationTableModel.setLocations(
                applicationConfigurationState.getGlobalLocations(),
                applicationConfigurationState.getActiveGlobalLocationIds());
    }

    JTextField getArtifactRepositoryBaseUrlOverrideField() {
        return artifactRepositoryBaseUrlOverrideField;
    }

    JTextField getArtifactRepositoryOverrideUsernameField() {
        return artifactRepositoryOverrideUsernameField;
    }

    JPasswordField getArtifactRepositoryOverridePasswordField() {
        return artifactRepositoryOverridePasswordField;
    }

    @Nullable
    private static String normalise(@Nullable final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @NotNull
    private static String emptyIfNull(@Nullable final String value) {
        return value == null ? "" : value;
    }

    private @NotNull JComponent createGlobalRulesEditor() {
        if (ApplicationManager.getApplication() == null) {
            final JScrollPane scrollPane = new JScrollPane(globalLocationTable);
            scrollPane.setPreferredSize(DECORATOR_DIMENSIONS);
            return scrollPane;
        }
        final ToolbarDecorator tableDecorator = ToolbarDecorator.createDecorator(globalLocationTable);
        addActionButtons(tableDecorator);
        tableDecorator.setPreferredSize(DECORATOR_DIMENSIONS);
        return tableDecorator.createPanel();
    }

    private void addActionButtons(ToolbarDecorator tableDecorator) {
        tableDecorator.setAddAction(button -> {
            final GlobalLocationDialogue dialogue = new GlobalLocationDialogue(null);
            if (dialogue.showAndGet()) {
                final GlobalConfigurationLocation newLocation = dialogue.getGlobalConfigurationLocation();
                if (newLocation != null) {
                    globalLocationTableModel.addLocation(newLocation);
                }
            }
        });
        tableDecorator.setEditAction(button -> {
            final int selectedRow = globalLocationTable.getSelectedRow();
            if (selectedRow >= 0) {
                final GlobalLocationDialogue dialogue = new GlobalLocationDialogue(
                        globalLocationTableModel.getLocationAt(selectedRow));
                if (dialogue.showAndGet()) {
                    final GlobalConfigurationLocation updated = dialogue.getGlobalConfigurationLocation();
                    if (updated != null) {
                        globalLocationTableModel.updateLocationAt(selectedRow, updated);
                    }
                }
            }
        });
        tableDecorator.setRemoveAction(button -> {
            final int selectedRow = globalLocationTable.getSelectedRow();
            if (selectedRow >= 0) {
                globalLocationTableModel.removeLocationAt(selectedRow);
            }
        });
<<<<<<< HEAD
        tableDecorator.setPreferredSize(DECORATOR_DIMENSIONS);

        return tableDecorator.createPanel();
=======
>>>>>>> 3b0fead4 (Refactor CheckStyleApplicationConfigurable)
    }

}


