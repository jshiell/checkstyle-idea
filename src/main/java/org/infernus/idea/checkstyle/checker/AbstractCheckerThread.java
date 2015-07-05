package org.infernus.idea.checkstyle.checker;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;
import org.infernus.idea.checkstyle.util.ModuleClassPathBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class AbstractCheckerThread extends Thread {

    private static final Log LOG = LogFactory.getLog(AbstractCheckerThread.class);

    private final List<PsiFile> files = new ArrayList<>();
    private final Map<Module, Set<PsiFile>> moduleToFiles = new HashMap<>();
    private final CheckStylePlugin plugin;
    private final ModuleClassPathBuilder moduleClassPathBuilder;
    private final ConfigurationLocation overrideConfigLocation;

    private Map<PsiFile, List<ProblemDescriptor>> fileResults;
    private ConfigurationLocationStatus configurationLocationStatus = ConfigurationLocationStatus.PRESENT;
    private boolean running = true;

    public AbstractCheckerThread(@NotNull final CheckStylePlugin checkStylePlugin,
                                 @NotNull final ModuleClassPathBuilder moduleClassPathBuilder,
                                 @NotNull final List<VirtualFile> virtualFiles,
                                 @Nullable final ConfigurationLocation overrideConfigLocation) {
        this.plugin = checkStylePlugin;
        this.moduleClassPathBuilder = moduleClassPathBuilder;
        this.overrideConfigLocation = overrideConfigLocation;

        final PsiManager psiManager = PsiManager.getInstance(this.plugin.getProject());
        for (final VirtualFile virtualFile : virtualFiles) {
            buildFilesList(psiManager, virtualFile);
        }

        for (final PsiFile file : files) {
            final Module module = ModuleUtil.findModuleForPsiElement(file);
            Set<PsiFile> filesForModule = moduleToFiles.get(module);
            if (filesForModule == null) {
                filesForModule = new HashSet<>();
                moduleToFiles.put(module, filesForModule);
            }
            filesForModule.add(file);
        }
    }

    protected Map<PsiFile, List<ProblemDescriptor>> getFileResults() {
        return fileResults;
    }

    protected void setFileResults(final Map<PsiFile, List<ProblemDescriptor>> fileResults) {
        this.fileResults = fileResults;
    }

    protected List<PsiFile> getFiles() {
        return files;
    }

    @Nullable
    protected CheckStyleToolWindowPanel toolWindowPanel() {
        return CheckStyleToolWindowPanel.panelFor(plugin.getProject());
    }

    protected void markThreadComplete() {
        plugin.setThreadComplete(this);
    }

    protected synchronized boolean isRunning() {
        return running;
    }

    protected synchronized void setRunning(final boolean running) {
        this.running = running;
    }

    protected ConfigurationLocationStatus getConfigurationLocationStatus() {
        return configurationLocationStatus;
    }

    public void stopCheck() {
        setRunning(false);
    }

    /**
     * Process each virtual file, adding to the map or finding children if a container.
     *
     * @param psiManager  the current manager.
     * @param virtualFile the file to process.
     */
    private void buildFilesList(final PsiManager psiManager, final VirtualFile virtualFile) {
        ApplicationManager.getApplication().runReadAction(() -> {
            VfsUtilCore.visitChildrenRecursively(virtualFile, new VirtualFileVisitor() {
                @Override
                public boolean visitFile(@NotNull final VirtualFile file) {
                    if (!file.isDirectory()) {
                        final PsiFile psiFile = psiManager.findFile(virtualFile);
                        if (psiFile != null) {
                            files.add(psiFile);
                        }
                    }
                    return true;
                }
            });
        });
    }

    protected void processFilesForModuleInfoAndScan() throws Throwable {
        for (final Module module : moduleToFiles.keySet()) {
            if (!isRunning()) {
                break;
            }

            if (module == null) {
                continue;
            }

            final Set<PsiFile> filesForModule = moduleToFiles.get(module);

            final ClassLoader moduleClassLoader = moduleClassPathBuilder.build(module);

            final FileScanner fileScanner = new FileScanner(plugin, filesForModule, moduleClassLoader, overrideConfigLocation);
            this.runFileScanner(fileScanner);

            configurationLocationStatus = fileScanner.getConfigurationLocationStatus();

            //noinspection ThrowableResultOfMethodCallIgnored
            if (fileScanner.getError() != null) {
                // throw any exceptions from the thread
                throw fileScanner.getError();
            }

            // add results if necessary
            if (fileScanner.getResults() != null) {
                for (final PsiFile psiFile : filesForModule) {
                    final List<ProblemDescriptor> resultsForFile = fileScanner.getResults().get(psiFile);
                    if (resultsForFile != null && !resultsForFile.isEmpty()) {
                        getFileResults().put(psiFile, new ArrayList<>(resultsForFile));
                    }
                }
            } else {
                LOG.warn("No results found for scan");
            }
        }
    }

    public abstract void runFileScanner(FileScanner fileScanner);

}
