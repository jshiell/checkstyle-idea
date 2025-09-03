package org.infernus.idea.checkstyle;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.BoundSearchableConfigurable;
import com.intellij.openapi.ui.DialogPanel;
import com.intellij.platform.ide.progress.ModalTaskOwner;
import com.intellij.platform.ide.progress.TaskCancellation;
import com.intellij.platform.ide.progress.TasksKt;
import com.intellij.ui.dsl.builder.Align;
import com.intellij.ui.dsl.builder.AlignX;
import com.intellij.ui.dsl.builder.BuilderKt;
import com.intellij.ui.dsl.builder.TextFieldKt;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
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
import javax.swing.SwingConstants;
import javax.swing.table.TableCellEditor;
import org.infernus.idea.checkstyle.config.ApplicationConfigurationState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CheckstyleDownloadsConfigurable extends BoundSearchableConfigurable {

    private static final VersionListReader VERSION_LIST_READER = new VersionListReader();

    private final Set<String> initialVersions = new HashSet<>();

    private final List<CheckstyleVersionDownload> versions = new ArrayList<>();


    public CheckstyleDownloadsConfigurable() {
        super("Checkstyle Downloader", "reference.settings.checkstyleDownloader",
            "org.infernus.idea.checkstyle.CheckstyleDownloadsConfigurable");
    }

    @Override
    public @NotNull DialogPanel createPanel() {
        final var applicationConfigurationState = ApplicationManager.getApplication()
            .getService(ApplicationConfigurationState.class);

        return BuilderKt.panel(panel -> {
            panel.row("Base Download URL: ", row -> {
                TextFieldKt.bindText(row.textField(),
                    () -> applicationConfigurationState.getState().getBaseDownloadUrl(), value -> {
                        applicationConfigurationState.getState().setBaseDownloadUrl(value);
                        return null;
                    }).align(AlignX.FILL);

                return null;
            });

            panel.row("Cache Path: ", row -> {
                TextFieldKt.bindText(row.textField(),
                    () -> applicationConfigurationState.getState().getCachePath().toString(),
                    value -> {
                        applicationConfigurationState.getState().setCachePath(value);
                        return null;
                    }).align(AlignX.FILL);

                return null;
            });

            panel.row("", row -> {
                final var table = new TableView<CheckstyleVersionDownload>(
                    new ListTableModel<>(createDownloadedColumn(), createVersionColumn()));
                table.setFillsViewportHeight(true);
                table.setStriped(true);

                try {
                    final var downloadedVersions = getDownloadedVersions(
                        ApplicationManager.getApplication()
                            .getService(ApplicationConfigurationState.class).getState()
                            .getCachePath());
                    initialVersions.addAll(downloadedVersions);
                    versions.addAll(VERSION_LIST_READER.getSupportedVersions().stream().map(
                        version -> new CheckstyleVersionDownload(
                            downloadedVersions.contains(version), version)).toList());
                    Collections.sort(versions);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }

                table.getListTableModel().setItems(versions);

                row.scrollCell(table).align(Align.FILL).onApply(() -> {
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
                    TasksKt.runWithModalProgressBlocking(ModalTaskOwner.component(table),
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

                    TasksKt.runWithModalProgressBlocking(ModalTaskOwner.component(table),
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

                    return null;
                }).onIsModified(() -> {
                    final Set<String> modifiedVersions = versions.stream()
                        .filter(version -> version.isDownloaded).map(version -> version.version)
                        .collect(Collectors.toSet());
                    if (!Objects.equals(initialVersions, modifiedVersions)) {
                        return true;
                    }

                    return false;
                }).onReset(() -> {

                    return null;
                });

                return null;
            });

            return null;
        });
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
            public TableCellEditor getEditor(CheckstyleVersionDownload checkstyleVersionDownload) {
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
            public @Nullable Boolean valueOf(CheckstyleVersionDownload checkstyleVersionDownload) {
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
            public @Nullable String valueOf(CheckstyleVersionDownload checkstyleVersionDownload) {
                if (checkstyleVersionDownload == null) {
                    return null;
                }
                return checkstyleVersionDownload.version;
            }
        };
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
