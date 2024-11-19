package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.infernus.idea.checkstyle.config.ConfigurationLocationSource;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.exception.CheckStylePluginParseException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ScanResult;
import org.infernus.idea.checkstyle.util.Notifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static org.infernus.idea.checkstyle.checker.ConfigurationLocationResult.of;
import static org.infernus.idea.checkstyle.checker.ConfigurationLocationStatus.BLOCKED;
import static org.infernus.idea.checkstyle.checker.ConfigurationLocationStatus.PRESENT;


public class ScanFiles implements Callable<List<ScanResult>> {

    private static final Logger LOG = Logger.getInstance(ScanFiles.class);

    private final List<PsiFile> files;
    private final Map<Module, Set<PsiFile>> moduleToFiles;
    private final Set<ScannerListener> listeners = new CopyOnWriteArraySet<>();
    private final Project project;
    @Nullable
    private final ConfigurationLocation overrideConfigLocation;

    public ScanFiles(@NotNull final Project project,
                     @NotNull final List<VirtualFile> virtualFiles,
                     @Nullable final ConfigurationLocation overrideConfigLocation) {
        this.project = project;
        this.overrideConfigLocation = overrideConfigLocation;

        files = findAllFilesFor(virtualFiles);
        moduleToFiles = mapsModulesToFiles();
    }

    private List<PsiFile> findAllFilesFor(@NotNull final List<VirtualFile> virtualFiles) {
        final List<PsiFile> childFiles = new ArrayList<>();
        final PsiManager psiManager = PsiManager.getInstance(project);
        for (final VirtualFile virtualFile : virtualFiles) {
            childFiles.addAll(buildFilesList(psiManager, virtualFile));
        }
        return childFiles;
    }

    private Map<Module, Set<PsiFile>> mapsModulesToFiles() {
        return ReadAction.compute(() -> {
            final Map<Module, Set<PsiFile>> modulesToFiles = new HashMap<>();
            for (final PsiFile file : files) {
                final Module module = ModuleUtil.findModuleForPsiElement(file);
                Set<PsiFile> filesForModule = modulesToFiles.computeIfAbsent(module, key -> new HashSet<>());
                filesForModule.add(file);
            }
            return modulesToFiles;
        });
    }

    @Override
    public final List<ScanResult> call() {
        try {
            fireCheckStarting(files);
            final List<ScanResult> scanResults = processFilesForModuleInfoAndScan();
            return scanCompletedSuccessfully(scanResults);

        } catch (CheckStylePluginParseException e) {
            LOG.debug("Parse exception caught during scan", e);
            return scanFailedWithError(e, false);
        } catch (final CheckStylePluginException e) {
            LOG.warn("An error occurred while scanning a file.", e);
            return scanFailedWithError(e, false);
        } catch (final Throwable e) {
            LOG.warn("An error occurred while scanning a file.", e);
            return scanFailedWithError(new CheckStylePluginException("An error occurred while scanning a file.", e), true);
        }
    }

    private List<ScanResult> scanFailedWithError(
            final CheckStylePluginException e,
            final boolean recordExceptionInEventLog) {
        if (recordExceptionInEventLog) {
            Notifications.showException(project, e);
        }
        fireScanFailedWithError(e);

        return List.of(ScanResult.EMPTY);
    }

    private List<ScanResult> scanCompletedSuccessfully(final List<ScanResult> results) {
        fireScanCompletedSuccessfully(results);
        return results;
    }

    public void addListener(final ScannerListener listener) {
        listeners.add(listener);
    }

    private void fireCheckStarting(final List<PsiFile> filesToScan) {
        listeners.forEach(listener -> listener.scanStarting(filesToScan));
    }

    private void fireScanCompletedSuccessfully(final List<ScanResult> scanResults) {
        listeners.forEach(listener -> listener.scanCompletedSuccessfully(scanResults));
    }

    private void fireScanFailedWithError(final CheckStylePluginException error) {
        listeners.forEach(listener -> listener.scanFailedWithError(error));
    }

    private void fireFilesScanned(final int count) {
        listeners.forEach(listener -> listener.filesScanned(count));
    }

    private List<PsiFile> buildFilesList(final PsiManager psiManager, final VirtualFile virtualFile) {
        return ReadAction.compute(() -> {
            final FindChildFiles visitor = new FindChildFiles(virtualFile, psiManager);
            VfsUtilCore.visitChildrenRecursively(virtualFile, visitor);
            return visitor.locatedFiles;
        });
    }

    private List<ScanResult> processFilesForModuleInfoAndScan() {
        final List<ScanResult> scanResults = new ArrayList<>();

        for (final Module module : moduleToFiles.keySet()) {
            if (module == null) {
                continue;
            }

            final List<ConfigurationLocationResult> locationResults = configurationLocation(overrideConfigLocation, module);
            if (locationResults.isEmpty()) {
                return List.of(new ScanResult(ConfigurationLocationResult.NOT_PRESENT, module, emptyMap()));
            }

            final Set<PsiFile> filesForModule = moduleToFiles.get(module);
            if (filesForModule.isEmpty()) {
                continue;
            }

            final List<ConfigurationLocation> locationsToCheck = locationResults.stream()
                    .filter(configurationLocationResult -> configurationLocationResult.status() != BLOCKED)
                    .map(ConfigurationLocationResult::location)
                    .collect(Collectors.toList());

            scanResults.addAll(checkFiles(module, filesForModule, locationsToCheck));

            fireFilesScanned(filesForModule.size());
        }

        return scanResults;
    }

    @NotNull
    private List<ConfigurationLocationResult> configurationLocation(
            final ConfigurationLocation override,
            final Module module) {
        final SortedSet<ConfigurationLocation> locations =
                configurationLocationSource().getConfigurationLocations(module, override);

        return locations.stream().map(it -> it.isBlocked()
                        ? of(it, BLOCKED)
                        : of(it, PRESENT))
                .collect(Collectors.toList());
    }

    private List<ScanResult> checkFiles(final Module module,
                                        final Set<PsiFile> filesToScan,
                                        final List<ConfigurationLocation> configurationLocations) {
        final List<ScannableFile> scannableFiles = new ArrayList<>();
        try {
            scannableFiles.addAll(ScannableFile.createAndValidate(filesToScan, module.getProject(), module, this.overrideConfigLocation));

            final List<ScanResult> scanResults = new ArrayList<>();
            for (ConfigurationLocation configurationLocation : configurationLocations) {
                var checker = checkerFactory().checker(module, configurationLocation);
                if (checker.isPresent()) {
                    var problems = checker.get().scan(scannableFiles, configurationManager().getCurrent().isSuppressErrors());
                    scanResults.add(new ScanResult(ConfigurationLocationResult.of(configurationLocation, PRESENT), module, problems));
                } else {
                    throw new CheckStylePluginException("Could not create checker for location " + configurationLocation);
                }
            }
            return scanResults;

        } finally {
            scannableFiles.forEach(ScannableFile::deleteIfRequired);
        }
    }

    private CheckerFactory checkerFactory() {
        return project.getService(CheckerFactory.class);
    }

    private PluginConfigurationManager configurationManager() {
        return project.getService(PluginConfigurationManager.class);
    }

    private ConfigurationLocationSource configurationLocationSource() {
        return project.getService(ConfigurationLocationSource.class);
    }

    private static class FindChildFiles extends VirtualFileVisitor {

        private final VirtualFile virtualFile;
        private final PsiManager psiManager;

        private final List<PsiFile> locatedFiles = new ArrayList<>();

        FindChildFiles(final VirtualFile virtualFile, final PsiManager psiManager) {
            this.virtualFile = virtualFile;
            this.psiManager = psiManager;
        }

        @Override
        public boolean visitFile(@NotNull final VirtualFile file) {
            if (!file.isDirectory()) {
                final PsiFile psiFile = psiManager.findFile(virtualFile);
                if (psiFile != null) {
                    locatedFiles.add(psiFile);
                }
            }
            return true;
        }
    }
}
