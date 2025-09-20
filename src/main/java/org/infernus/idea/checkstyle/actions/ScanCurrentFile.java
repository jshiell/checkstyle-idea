package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import org.infernus.idea.checkstyle.config.PluginConfiguration;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.infernus.idea.checkstyle.util.Async;
import org.infernus.idea.checkstyle.util.FileTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.infernus.idea.checkstyle.actions.ToolWindowAccess.toolWindow;

/**
 * Action to execute a CheckStyle scan on the current editor file.
 */
public class ScanCurrentFile extends BaseAction {
    private static final Logger LOG = Logger.getInstance(ScanCurrentFile.class);

    @Override
    public void actionPerformed(final @NotNull AnActionEvent event) {
        project(event).ifPresent(project -> {
            try {
                final ToolWindow toolWindow = toolWindow(project);
                toolWindow.activate(() -> {
                    try {
                        final VirtualFile selectedFile = selectedFile(project, event);

                        setProgressText(toolWindow, "plugin.status.in-progress.current");

                        Async.executeOnPooledThread(() -> {
                            final ConfigurationLocation overrideIfExists = getSelectedOverride(toolWindow);
                            if (validForScanning(selectedFile, project, overrideIfExists)) {
                                staticScanner(project).asyncScanFiles(
                                        singletonList(selectedFile),
                                        overrideIfExists);
                            } else {
                                ApplicationManager.getApplication().invokeLater(() ->
                                        setProgressText(toolWindow, "plugin.status.in-progress.out-of-scope"));
                            }
                            return null;
                        });

                    } catch (Throwable e) {
                        LOG.warn("Current File scan failed", e);
                    }
                });

            } catch (Throwable e) {
                LOG.warn("Current File scan failed", e);
            }
        });
    }

    protected @Nullable VirtualFile selectedFile(@NotNull final Project project,
                                                 @NotNull final AnActionEvent event) {
        return getSelectedFile(project);
    }

    protected boolean validForScanning(final VirtualFile selectedFile,
                                       final @NotNull Project project,
                                       final @Nullable ConfigurationLocation overrideIfExists) {
        if (selectedFile == null) {
            return false;
        }

        final PluginConfiguration pluginConfiguration = configurationManager(project).getCurrent();
        if (!isFileValidAgainstScanScope(project, pluginConfiguration, selectedFile)) {
            return false;
        }

        final List<NamedScope> namedScopes = getNamedScopesToCheck(pluginConfiguration, overrideIfExists);
        if (!namedScopes.isEmpty() && namedScopes.stream().map(NamedScope::getValue).allMatch(Objects::isNull)) {
            return true;
        }

        return namedScopes.stream()
                .anyMatch((NamedScope namedScope) -> NamedScopeHelper.isFileInScope(psiFileFor(project, selectedFile), namedScope));
    }

    @NotNull
    private static PsiFile psiFileFor(@NotNull final Project project,
                                      @NotNull final VirtualFile selectedFile) {
        return ReadAction.compute(() -> {
            final PsiFile psiFile = PsiManager.getInstance(project).findFile(selectedFile);
            if (psiFile == null) {
                throw new UnsupportedOperationException("PsiFile of " + selectedFile + " is null!");
            }
            return psiFile;
        });
    }

    private static boolean isFileValidAgainstScanScope(@NotNull final Project project,
                                                       @NotNull final PluginConfiguration pluginConfiguration,
                                                       @NotNull final VirtualFile selectedFile) {
        final ScanScope scanScope = pluginConfiguration.getScanScope();

        if (scanScope != ScanScope.Everything) {
            return ReadAction.compute(() -> {
                final ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(project);
                if (!projectFileIndex.isInSourceContent(selectedFile)) {
                    return false;
                }
                if (!scanScope.includeNonJavaSources() && !FileTypes.isJava(selectedFile.getFileType())) {
                    return false;
                }
                if (!scanScope.includeTestClasses()) {
                    return !projectFileIndex.isInTestSourceContent(selectedFile);
                }
                return true;
            });
        }

        return true;
    }

    @Nullable
    private VirtualFile getSelectedFile(@NotNull final Project project) {
        VirtualFile selectedFile = null;

        final Editor selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (selectedTextEditor != null) {
            selectedFile = FileDocumentManager.getInstance().getFile(selectedTextEditor.getDocument());
        }

        if (selectedFile == null) {
            // this is the preferred solution, but it doesn't respect the focus of split editors at present
            final VirtualFile[] selectedFiles = FileEditorManager.getInstance(project).getSelectedFiles();
            if (selectedFiles.length > 0) {
                selectedFile = selectedFiles[0];
            }
        }
        return selectedFile;
    }

    /**
     * Returns the NamedScopes that are to be checked. If overrideIfExists is provided, only its Scope is returned.
     * Otherwise, all {@link PluginConfiguration#getActiveLocationIds() activeLocations} of the provided pluginConfiguration
     * are returned.
     */
    @NotNull
    private List<NamedScope> getNamedScopesToCheck(final PluginConfiguration pluginConfiguration,
                                                   final @Nullable ConfigurationLocation overrideIfExists) {
        final Collection<ConfigurationLocation> getLocationsToCheck;
        if (overrideIfExists != null) {
            getLocationsToCheck = singletonList(overrideIfExists);
        } else {
            getLocationsToCheck = pluginConfiguration.getActiveLocations();
        }
        return getLocationsToCheck.stream()
                .map(ConfigurationLocation::getNamedScope)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    @Override
    public void update(final @NotNull AnActionEvent event) {
        final Presentation presentation = event.getPresentation();

        project(event).ifPresentOrElse(project -> {
            final VirtualFile selectedFile = selectedFile(project, event);
            if (selectedFile != null) {
                presentation.setEnabled(!staticScanner(project).isScanInProgress());
            } else {
                presentation.setEnabled(false);
            }
        }, () -> presentation.setEnabled(false));
    }
}
