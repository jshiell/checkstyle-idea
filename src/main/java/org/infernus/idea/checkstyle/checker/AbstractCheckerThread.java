package org.infernus.idea.checkstyle.checker;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStylePlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract CheckerThread.
 */
public abstract class AbstractCheckerThread extends Thread {

    private static final Log LOG = LogFactory.getLog(AbstractCheckerThread.class);

    /**
     * Files to scan.
     */
    private final List<PsiFile> files = new ArrayList<PsiFile>();

    /**
     * Map modules to files.
     */
    private final Map<Module, List<PsiFile>> moduleToFiles = new HashMap<Module, List<PsiFile>>();

    /**
     * Scan results.
     */
    private Map<PsiFile, List<ProblemDescriptor>> fileResults;

    private boolean running = true;

    private CheckStylePlugin plugin;

    public AbstractCheckerThread(final CheckStylePlugin checkStylePlugin,
                                 final List<VirtualFile> virtualFiles) {
        this.plugin = checkStylePlugin;

        if (files == null) {
            throw new IllegalArgumentException("Files may not be null.");
        }
        // this needs to be done on the main thread.
        final PsiManager psiManager = PsiManager.getInstance(this.plugin.getProject());
        for (final VirtualFile virtualFile : virtualFiles) {
            buildFilesList(psiManager, virtualFile);
        }

        for (final PsiFile file : files) {
            final Module module = ModuleUtil.findModuleForPsiElement(file);
            List<PsiFile> filesForModule = moduleToFiles.get(module);
            if (filesForModule == null) {
                filesForModule = new ArrayList<PsiFile>();
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

    protected CheckStylePlugin getPlugin() {
        return plugin;
    }

    protected synchronized boolean isRunning() {
        return running;
    }

    protected synchronized void setRunning(final boolean running) {
        this.running = running;
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
        if (virtualFile.isDirectory()) {
            for (final VirtualFile child : virtualFile.getChildren()) {
                buildFilesList(psiManager, child);
            }

        } else {
            final PsiFile psiFile = psiManager.findFile(virtualFile);
            if (psiFile != null) {
                files.add(psiFile);
            }
        }
    }

    protected void processFilesForModuleInfoAndScan() throws Throwable {
        for (final Module module : moduleToFiles.keySet()) {
            if (!isRunning()) {
                break;
            }

            if (module == null) {
                continue;
            }

            final List<PsiFile> filesForModule = moduleToFiles.get(module);

            final ClassLoader moduleClassLoader = plugin.buildModuleClassLoader(module);

            final FileScanner fileScanner = new FileScanner(plugin, filesForModule, moduleClassLoader);
            this.runFileScanner(fileScanner);

            // check for errors
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
                        getFileResults().put(psiFile, new ArrayList<ProblemDescriptor>(resultsForFile));
                    }
                }
            } else {
                LOG.warn("No results found for scan");
            }
        }
    }

    public abstract void runFileScanner(FileScanner fileScanner) throws InterruptedException, InvocationTargetException;

}
