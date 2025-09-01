package org.infernus.idea.checkstyle;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.NlsContexts.ConfigurableName;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.ide.progress.ModalTaskOwner;
import com.intellij.platform.ide.progress.TaskCancellation;
import com.intellij.platform.ide.progress.TasksKt;
import com.intellij.ui.components.ComponentsKt;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListTableModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellEditor;
import org.infernus.idea.checkstyle.config.ApplicationConfigurationState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CheckstyleDownloadsConfigurable implements Configurable {

    private final ConfigPanel configPanel;

    CheckstyleDownloadsConfigurable() {
        this.configPanel = new ConfigPanel();
    }

    @Override
    public @ConfigurableName String getDisplayName() {
        return CheckStyleBundle.message("config-downloader.display-name");
    }

    @Override
    public @Nullable JComponent createComponent() {
        reset();
        return configPanel;
    }

    @Override
    public boolean isModified() {
        return configPanel.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        configPanel.apply();
    }

    @Override
    public void reset() {
        configPanel.clear();
        configPanel.initialize();
    }

    private static class ConfigPanel extends JPanel {

        private static final VersionListReader VERSION_LIST_READER = new VersionListReader();

        private final String initialBaseUrl;
        private final Path initialDownloadDirectory;
        private final Set<String> initialVersions = new HashSet<>();

        private final JBLabel baseUrlLabel;
        private final JBTextField baseUrlTextField;

        private final JBLabel downloadUrlLabel;
        private final TextFieldWithBrowseButton downloadDirectoryTextField;

        private final JPanel downloadsPanel;
        private final TableView<CheckstyleVersionDownload> downloadsTable;
        private final List<CheckstyleVersionDownload> versions = new ArrayList<>();

        ConfigPanel() {
            super(new GridBagLayout());
            final var applicationConfigurationState = ApplicationManager.getApplication()
                .getService(ApplicationConfigurationState.class);
            this.initialBaseUrl = applicationConfigurationState.getState().getBaseDownloadUrl();
            this.initialDownloadDirectory = applicationConfigurationState.getState().getCachePath();

            this.baseUrlLabel = new JBLabel("Base URL:");
            this.baseUrlTextField = new JBTextField();

            this.downloadUrlLabel = new JBLabel("Download URL:");
            this.downloadDirectoryTextField = new TextFieldWithBrowseButton();

            ComponentsKt.installFileCompletionAndBrowseDialog(null, this.downloadDirectoryTextField,
                this.downloadDirectoryTextField.getTextField(), "My Title", "My description",
                FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT, VirtualFile::getPath);

            downloadsTable = new TableView<>(
                new ListTableModel<>(createDownloadedColumn(), createVersionColumn()));
            this.downloadsPanel = new JPanel(new BorderLayout());
            this.downloadsPanel.add(new JBScrollPane(downloadsTable), BorderLayout.CENTER);
        }

        void apply() {
            final var applicationConfigurationState = ApplicationManager.getApplication()
                .getService(ApplicationConfigurationState.class);
            applicationConfigurationState.setBaseDownloadUrl(baseUrlTextField.getText());
            applicationConfigurationState.setCachePath(
                Path.of(this.downloadDirectoryTextField.getText()));

            // Must come after the base URL and cache path changes are persisted to the
            // application state to pick up any changes made there first.
            final Set<String> downloadedVersions;
            try {
                downloadedVersions = getDownloadedVersions(
                    applicationConfigurationState.getState().getCachePath());
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }

            final Set<String> versionsToDownload = new HashSet<>();
            final Set<String> versionsToRemove = new HashSet<>();
            for (final CheckstyleVersionDownload version : versions) {
                // Literally exists on the file system already.
                final boolean isAlreadyDownloaded = downloadedVersions.contains(
                    version.version);
                // Selected in the UI as a desired version.
                final boolean userIndicatedShouldBeDownloaded = version.isDownloaded;

                if (!isAlreadyDownloaded && userIndicatedShouldBeDownloaded) {
                    versionsToDownload.add(version.version);
                } else if (isAlreadyDownloaded && !userIndicatedShouldBeDownloaded) {
                    versionsToRemove.add(version.version);
                }
            }

            final var checkstyleDownloader = ApplicationManager.getApplication()
                .getService(CheckstyleDownloader.class);
            TasksKt.runWithModalProgressBlocking(ModalTaskOwner.component(this),
                "Downloading Checkstyle Versions", TaskCancellation.nonCancellable(),
                (scope, continuation) -> {
                    versionsToDownload.forEach(version -> {
                        try {
                            checkstyleDownloader.downloadVersion(version);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });

                    return null;
                });

            TasksKt.runWithModalProgressBlocking(ModalTaskOwner.component(this),
                "Deleting Checkstyle Versions", TaskCancellation.nonCancellable(),
                (scope, continuation) -> {
                    versionsToRemove.forEach(version -> {
                        try {
                            checkstyleDownloader.deleteVersion(version);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });

                    return null;
                });

        }

        void clear() {
            versions.clear();
            downloadsTable.getListTableModel().setItems(versions);

            remove(baseUrlLabel);
            remove(baseUrlTextField);
            remove(downloadUrlLabel);
            remove(downloadDirectoryTextField);
            remove(downloadsPanel);
        }

        boolean isModified() {
            if (!Objects.equals(initialBaseUrl, baseUrlTextField.getText())) {
                return true;
            }
            if (!Objects.equals(initialDownloadDirectory,
                Path.of(downloadDirectoryTextField.getText()))) {
                return true;
            }
            final Set<String> modifiedVersions = versions.stream()
                .filter(version -> version.isDownloaded).map(version -> version.version)
                .collect(Collectors.toSet());
            if (!Objects.equals(initialVersions, modifiedVersions)) {
                return true;
            }

            return false;
        }

        void initialize() {
            try {
                final var downloadedVersions = getDownloadedVersions(
                    ApplicationManager.getApplication()
                        .getService(ApplicationConfigurationState.class).getState()
                        .getCachePath());
                initialVersions.addAll(downloadedVersions);
                versions.addAll(VERSION_LIST_READER.getSupportedVersions().stream().map(
                    version -> new CheckstyleVersionDownload(downloadedVersions.contains(version),
                        version)).toList());
                Collections.sort(versions);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            downloadsTable.getListTableModel().setItems(versions);

            final var applicationConfigurationState = ApplicationManager.getApplication()
                .getService(ApplicationConfigurationState.class);
            baseUrlTextField.setText(applicationConfigurationState.getState().getBaseDownloadUrl());
            downloadDirectoryTextField.setText(
                applicationConfigurationState.getState().getCachePath().toString());

            add(baseUrlLabel, new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, JBUI.insets(4), 0, 0));
            add(baseUrlTextField,
                new GridBagConstraints(2, 0, 2, 1, 1.0, 1.0, GridBagConstraints.WEST,
                    GridBagConstraints.HORIZONTAL, JBUI.insets(4), 0, 0));

            add(downloadUrlLabel,
                new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0, GridBagConstraints.WEST,
                    GridBagConstraints.HORIZONTAL, JBUI.insets(4), 0, 0));
            add(downloadDirectoryTextField,
                new GridBagConstraints(2, 1, 2, 1, 1.0, 1.0, GridBagConstraints.WEST,
                    GridBagConstraints.HORIZONTAL, JBUI.insets(4), 0, 0));

            add(downloadsPanel,
                new GridBagConstraints(0, 2, 4, 1, 1.0, 1.0, GridBagConstraints.WEST,
                    GridBagConstraints.BOTH, JBUI.insets(4), 0, 0));
        }

        private Set<String> getDownloadedVersions(final Path cachePath) throws IOException {
            if (!Files.exists(cachePath)) {
                return Collections.emptySet();
            }
            try (final var cachedFiles = Files.list(cachePath)) {
                return cachedFiles.map(path -> {
                    final String fileName = path.getFileName().toString();
                    if (!fileName.startsWith("checkstyle-") && !fileName.endsWith("-all.jar")) {
                        return null;
                    }
                    return fileName.substring("checkstyle-".length(),
                        fileName.length() - "-all.jar".length());
                }).filter(Objects::nonNull).collect(Collectors.toSet());
            }
        }

        private static ColumnInfo<CheckstyleVersionDownload, Boolean> createDownloadedColumn() {
            return new ColumnInfo<>(
                CheckStyleBundle.message("config-downloader.downloads.table.downloaded")) {

                @Override
                public Class<Boolean> getColumnClass() {
                    return Boolean.class;
                }

                @Override
                public TableCellEditor getEditor(
                    CheckstyleVersionDownload checkstyleVersionDownload) {
                    final var checkbox = new JCheckBox();
                    checkbox.setHorizontalAlignment(SwingConstants.CENTER);
                    return new DefaultCellEditor(checkbox);
                }

                @Override
                public boolean isCellEditable(CheckstyleVersionDownload checkstyleVersionDownload) {
                    return true;
                }

                @Override
                public void setValue(CheckstyleVersionDownload checkstyleVersionDownload,
                    Boolean value) {
                    if (checkstyleVersionDownload != null && value != null) {
                        checkstyleVersionDownload.isDownloaded = value;
                    }
                }

                @Override
                public @Nullable Boolean valueOf(
                    CheckstyleVersionDownload checkstyleVersionDownload) {
                    if (checkstyleVersionDownload == null) {
                        return null;
                    }
                    return checkstyleVersionDownload.isDownloaded;
                }
            };
        }

        private static ColumnInfo<CheckstyleVersionDownload, String> createVersionColumn() {
            return new ColumnInfo<>(
                CheckStyleBundle.message("config-downloader.downloads.table.version")) {
                @Override
                public @Nullable String valueOf(
                    CheckstyleVersionDownload checkstyleVersionDownload) {
                    if (checkstyleVersionDownload == null) {
                        return null;
                    }
                    return checkstyleVersionDownload.version;
                }
            };
        }
    }

    private static class CheckstyleVersionDownload implements
        Comparable<CheckstyleVersionDownload> {

        private boolean isDownloaded;
        private String version;

        CheckstyleVersionDownload(final boolean isDownloaded, final String version) {
            this.isDownloaded = isDownloaded;
            this.version = version;
        }

        private static final Comparator<String> VERSION_COMPARATOR = new VersionComparator().reversed();

        @Override
        public int compareTo(@NotNull final CheckstyleVersionDownload o) {
            final var versionCompare = Objects.compare(version, o.version, VERSION_COMPARATOR);
            if (versionCompare != 0) {
                return versionCompare;
            }
            return Objects.compare(isDownloaded, o.isDownloaded, Boolean::compareTo);
        }
    }
}
