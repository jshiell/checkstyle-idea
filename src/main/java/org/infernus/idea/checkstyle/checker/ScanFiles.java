package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
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
import org.infernus.idea.checkstyle.util.Notifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.intellij.openapi.util.Pair.pair;
import static java.util.Collections.emptyMap;
import static org.infernus.idea.checkstyle.checker.ConfigurationLocationResult.resultOf;
import static org.infernus.idea.checkstyle.checker.ConfigurationLocationStatus.*;


public class ScanFiles implements Callable<Map<PsiFile, List<Problem>>> {

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
        final Map<Module, Set<PsiFile>> modulesToFiles = new HashMap<>();
        ApplicationManager.getApplication().runReadAction(() -> {
            for (final PsiFile file : files) {
                final Module module = ModuleUtil.findModuleForPsiElement(file);
                Set<PsiFile> filesForModule = modulesToFiles.computeIfAbsent(module, key -> new HashSet<>());
                filesForModule.add(file);
            }
        });
        return modulesToFiles;
    }

    @Override
    public final Map<PsiFile, List<Problem>> call() {
        try {
            fireCheckStarting(files);
            final Pair<ConfigurationLocationResult, Map<PsiFile, List<Problem>>> scanResult =
                    processFilesForModuleInfoAndScan();
            return scanCompletedSuccessfully(scanResult.first, scanResult.second);

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

    private Map<PsiFile, List<Problem>> scanFailedWithError(final CheckStylePluginException e,
                                                            final boolean recordExceptionInEventLog) {
        if (recordExceptionInEventLog) {
            Notifications.showException(project, e);
        }
        fireScanFailedWithError(e);

        return emptyMap();
    }

    private Map<PsiFile, List<Problem>> scanCompletedSuccessfully(final ConfigurationLocationResult configurationLocationResult,
                                                                  final Map<PsiFile, List<Problem>> filesToProblems) {
        fireScanCompletedSuccessfully(configurationLocationResult, filesToProblems);
        return filesToProblems;
    }

    public void addListener(final ScannerListener listener) {
        listeners.add(listener);
    }

    private void fireCheckStarting(final List<PsiFile> filesToScan) {
        listeners.forEach(listener -> listener.scanStarting(filesToScan));
    }

    private void fireScanCompletedSuccessfully(final ConfigurationLocationResult configLocationResult,
                                               final Map<PsiFile, List<Problem>> fileResults) {
        listeners.forEach(listener -> listener.scanCompletedSuccessfully(configLocationResult, fileResults));
    }

    private void fireScanFailedWithError(final CheckStylePluginException error) {
        listeners.forEach(listener -> listener.scanFailedWithError(error));
    }

    private void fireFilesScanned(final int count) {
        listeners.forEach(listener -> listener.filesScanned(count));
    }

    private List<PsiFile> buildFilesList(final PsiManager psiManager, final VirtualFile virtualFile) {
        final List<PsiFile> allChildFiles = new ArrayList<>();
        ApplicationManager.getApplication().runReadAction(() -> {
            final FindChildFiles visitor = new FindChildFiles(virtualFile, psiManager);
            VfsUtilCore.visitChildrenRecursively(virtualFile, visitor);
            allChildFiles.addAll(visitor.locatedFiles);
        });
        return allChildFiles;
    }

    private Pair<ConfigurationLocationResult, Map<PsiFile, List<Problem>>> processFilesForModuleInfoAndScan() {
        final Map<PsiFile, List<Problem>> fileResults = new HashMap<>();

        for (final Module module : moduleToFiles.keySet()) {
            if (module == null) {
                continue;
            }

            final List<ConfigurationLocationResult> locationResults = configurationLocation(overrideConfigLocation, module);
            if (locationResults.isEmpty()) {
                return pair(resultOf(NOT_PRESENT), emptyMap());
            }

            final Set<PsiFile> filesForModule = moduleToFiles.get(module);
            if (filesForModule.isEmpty()) {
                continue;
            }

            final List<ConfigurationLocation> locationsToCheck = locationResults.stream()
                    .filter(configurationLocationResult -> configurationLocationResult.status != BLOCKED)
                    .map(configurationLocationResult -> configurationLocationResult.location)
                    .collect(Collectors.toList());

            fileResults.putAll(filesWithProblems(filesForModule,
                    checkFiles(module, filesForModule, locationsToCheck)));
            fireFilesScanned(filesForModule.size());
        }

        return pair(resultOf(PRESENT), fileResults);
    }

    @NotNull
    private Map<PsiFile, List<Problem>> filesWithProblems(final Set<PsiFile> filesForModule,
                                                          final Map<PsiFile, List<Problem>> moduleFileResults) {
        final Map<PsiFile, List<Problem>> moduleResults = new HashMap<>();
        for (final PsiFile psiFile : filesForModule) {
            final List<Problem> resultsForFile = moduleFileResults.get(psiFile);
            if (resultsForFile != null && !resultsForFile.isEmpty()) {
                moduleResults.put(psiFile, new ArrayList<>(resultsForFile));
            }
        }
        return moduleResults;
    }

    @NotNull
    private List<ConfigurationLocationResult> configurationLocation(
            final ConfigurationLocation override,
            final Module module) {
        final SortedSet<ConfigurationLocation> locations =
                configurationLocationSource().getConfigurationLocations(module, override);

        return locations.stream().map(it -> it.isBlocked()
                        ? resultOf(it, BLOCKED)
                        : resultOf(it, PRESENT))
                .collect(Collectors.toList());
    }

    private Map<PsiFile, List<Problem>> checkFiles(final Module module,
                                                   final Set<PsiFile> filesToScan,
                                                   final List<ConfigurationLocation> configurationLocations) {
        final List<ScannableFile> scannableFiles = new ArrayList<>();
        try {
            scannableFiles.addAll(ScannableFile.createAndValidate(filesToScan, module.getProject(), module, this.overrideConfigLocation));

            return configurationLocations.stream().map(configurationLocation -> checkerFactory().checker(module, configurationLocation)
                    .map(checker -> checker.scan(scannableFiles, configurationManager().getCurrent().isSuppressErrors()))
                    .orElseThrow(() -> new CheckStylePluginException("Could not create checker")))
                    .flatMap(e -> e.entrySet().stream())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (List<Problem> e1, List<Problem> e2) -> {
                                // Merge function to join multiple list of problems for the same file
                                // If the same (equals) problem is contained in e1 and e2 only one gets added
                                return Stream.concat(e1.stream(), e2.stream())
                                        .distinct()
                                        .collect(Collectors.toList());
                            }));
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
