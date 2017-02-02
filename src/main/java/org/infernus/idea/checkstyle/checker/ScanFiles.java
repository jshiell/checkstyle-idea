package org.infernus.idea.checkstyle.checker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.RuntimeInterruptedException;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static com.intellij.openapi.util.Pair.pair;
import static java.util.Collections.emptyMap;
import static org.infernus.idea.checkstyle.checker.ConfigurationLocationResult.resultOf;
import static org.infernus.idea.checkstyle.checker.ConfigurationLocationStatus.BLACKLISTED;
import static org.infernus.idea.checkstyle.checker.ConfigurationLocationStatus.NOT_PRESENT;
import static org.infernus.idea.checkstyle.checker.ConfigurationLocationStatus.PRESENT;


public class ScanFiles
        implements Callable<Map<PsiFile, List<Problem>>>
{
    private static final Log LOG = LogFactory.getLog(ScanFiles.class);

    private final List<PsiFile> files;
    private final Map<Module, Set<PsiFile>> moduleToFiles;
    private final Set<ScannerListener> listeners = new HashSet<>();
    private final CheckStylePlugin plugin;
    private final ConfigurationLocation overrideConfigLocation;

    public ScanFiles(@NotNull final CheckStylePlugin checkStylePlugin, @NotNull final List<VirtualFile> virtualFiles,
                     @Nullable final ConfigurationLocation overrideConfigLocation) {
        this.plugin = checkStylePlugin;
        this.overrideConfigLocation = overrideConfigLocation;

        files = findAllFilesFor(virtualFiles);
        moduleToFiles = mapsModulesToFiles();
    }

    private List<PsiFile> findAllFilesFor(@NotNull final List<VirtualFile> virtualFiles) {
        final List<PsiFile> childFiles = new ArrayList<>();
        final PsiManager psiManager = PsiManager.getInstance(this.plugin.getProject());
        for (final VirtualFile virtualFile : virtualFiles) {
            childFiles.addAll(buildFilesList(psiManager, virtualFile));
        }
        return childFiles;
    }

    private Map<Module, Set<PsiFile>> mapsModulesToFiles() {
        final Map<Module, Set<PsiFile>> modulesToFiles = new HashMap<>();
        for (final PsiFile file : files) {
            final Module module = ModuleUtil.findModuleForPsiElement(file);
            Set<PsiFile> filesForModule = modulesToFiles.get(module);
            if (filesForModule == null) {
                filesForModule = new HashSet<>();
                modulesToFiles.put(module, filesForModule);
            }
            filesForModule.add(file);
        }
        return modulesToFiles;
    }

    @Override
    public final Map<PsiFile, List<Problem>> call() {
        try {
            fireCheckStarting(files);
            final Pair<ConfigurationLocationResult, Map<PsiFile, List<Problem>>> scanResult =
                    processFilesForModuleInfoAndScan();
            return checkComplete(scanResult.first, scanResult.second);
        } catch (final RuntimeInterruptedException e) {
            LOG.debug("Scan cancelled by IDEA", e);
            return checkComplete(resultOf(PRESENT), emptyMap());
        } catch (final CheckStylePluginException e) {
            LOG.error("An error occurred while scanning a file.", e);
            fireErrorCaught(e);
            return checkComplete(resultOf(PRESENT), emptyMap());
        } catch (final Throwable e) {
            LOG.error("An error occurred while scanning a file.", e);
            fireErrorCaught(new CheckStylePluginException("An error occurred while scanning a file.", e));
            return checkComplete(resultOf(PRESENT), emptyMap());
        }
    }

    private Map<PsiFile, List<Problem>> checkComplete(final ConfigurationLocationResult configurationLocationResult,
                                                      final Map<PsiFile, List<Problem>> filesToProblems) {
        fireCheckComplete(configurationLocationResult, filesToProblems);
        return filesToProblems;
    }

    public void addListener(ScannerListener listener) {
        listeners.add(listener);
    }

    private void fireCheckStarting(final List<PsiFile> filesToScan) {
        listeners.forEach(listener -> listener.scanStarting(filesToScan));
    }

    private void fireCheckComplete(final ConfigurationLocationResult configLocationResult, Map<PsiFile,
            List<Problem>> fileResults) {
        listeners.forEach(listener -> listener.scanComplete(configLocationResult, fileResults));
    }

    private void fireErrorCaught(final CheckStylePluginException error) {
        listeners.forEach(listener -> listener.errorCaught(error));
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

            final ConfigurationLocationResult locationResult = configurationLocation(overrideConfigLocation, module);
            if (locationResult.status != PRESENT) {
                return pair(locationResult, emptyMap());
            }

            final Set<PsiFile> filesForModule = moduleToFiles.get(module);
            if (filesForModule.isEmpty()) {
                continue;
            }

            fileResults.putAll(filesWithProblems(filesForModule, checkFiles(module, filesForModule, locationResult
                    .location)));
            fireFilesScanned(filesForModule.size());
        }

        return pair(resultOf(PRESENT), fileResults);
    }

    @NotNull
    private Map<PsiFile, List<Problem>> filesWithProblems(final Set<PsiFile> filesForModule, final Map<PsiFile,
            List<Problem>> moduleFileResults) {
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
    private ConfigurationLocationResult configurationLocation(final ConfigurationLocation override, final Module
            module) {
        final ConfigurationLocation location = plugin.getConfigurationLocation(module, override);
        if (location == null) {
            return resultOf(NOT_PRESENT);
        }
        if (location.isBlacklisted()) {
            return resultOf(location, BLACKLISTED);
        }
        return resultOf(location, PRESENT);
    }

    private Map<PsiFile, List<Problem>> checkFiles(final Module module, final Set<PsiFile> filesToScan, final
    ConfigurationLocation configurationLocation) {
        final List<ScannableFile> scannableFiles = new ArrayList<>();
        try {
            scannableFiles.addAll(ScannableFile.createAndValidate(filesToScan, plugin, module));

            return checkerFactory(module.getProject()).checker(module, configurationLocation).map(checker -> checker.scan
                    (scannableFiles, plugin.getConfiguration().isSuppressingErrors())).orElseGet(Collections::emptyMap);
        } finally {
            scannableFiles.forEach(ScannableFile::deleteIfRequired);
        }
    }

    private CheckerFactory checkerFactory(final Project project) {
        return ServiceManager.getService(project, CheckerFactory.class);
    }


    private class FindChildFiles
            extends VirtualFileVisitor
    {
        private final VirtualFile virtualFile;
        private final PsiManager psiManager;

        public final List<PsiFile> locatedFiles = new ArrayList<>();

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
