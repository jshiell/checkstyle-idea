package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.codeInspection.ProblemDescriptor;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.InvocationTargetException;

import org.infernus.idea.checkstyle.CheckStylePlugin;

/**
 * Abstract CheckerThread.
 */
public abstract class AbstractCheckerThread extends Thread {

    /**
     * Files to scan.
     */
    private final List<PsiFile> files = new ArrayList<PsiFile>();

    /**
     * Map files to modules.
     */
    private final Map<PsiFile, Module> fileToModuleMap = new HashMap<PsiFile, Module>();

    /**
     * Scan results.
     */
    private Map<PsiFile, List<ProblemDescriptor>> fileResults;

    private boolean running = true;

    private CheckStylePlugin plugin;

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

    public AbstractCheckerThread(CheckStylePlugin checkStylePlugin, final List<VirtualFile> virtualFiles) {
        this.plugin = checkStylePlugin;

        if (files == null) {
            throw new IllegalArgumentException("Files may not be null.");
        }
        // this needs to be done on the main thread.
        final PsiManager psiManager = PsiManager.getInstance(this.plugin.getProject());
        for (final VirtualFile virtualFile : virtualFiles) {
            processFile(psiManager, virtualFile);
        }

        // build module map (also on main frame)
        for (final PsiFile file : files) {
            this.fileToModuleMap.put(file, ModuleUtil.findModuleForPsiElement(file));
        }
    }

    /**
     * Process each virtual file, adding to the map or finding children if a container.
     *
     * @param psiManager  the current manager.
     * @param virtualFile the file to process.
     */
    private void processFile(final PsiManager psiManager, final VirtualFile virtualFile) {
        if (virtualFile.isDirectory()) {
            for (final VirtualFile child : virtualFile.getChildren()) {
                processFile(psiManager, child);
            }

        } else {
            final PsiFile psiFile = psiManager.findFile(virtualFile);
            if (psiFile != null) {
                files.add(psiFile);
            }
        }
    }

    protected void processFilesForModuleInfoAndScan() throws Throwable {
        final Map<Module, ClassLoader> moduleClassLoaderMap
                = new HashMap<Module, ClassLoader>();


        for (final PsiFile psiFile : files) {
            if (!isRunning()) {
                break;
            }

            if (psiFile == null) {
                continue;
            }

            final Module module = fileToModuleMap.get(psiFile);
            final ClassLoader moduleClassLoader;
            if (moduleClassLoaderMap.containsKey(module)) {
                moduleClassLoader = moduleClassLoaderMap.get(module);
            } else {
                moduleClassLoader = plugin.buildModuleClassLoader(module);
                moduleClassLoaderMap.put(module, moduleClassLoader);
            }

            final FileScanner fileScanner = new FileScanner(plugin, psiFile, moduleClassLoader);

            this.runFileScanner(fileScanner);

            // check for errors
            if (fileScanner.getError() != null) {
                // throw any exceptions from the thread
                throw fileScanner.getError();
            }

            // add results if necessary
            if (fileScanner.getResults() != null
                    && fileScanner.getResults().size() > 0) {
                getFileResults().put(psiFile, fileScanner.getResults());
            }
        }
    }

    public abstract void runFileScanner(FileScanner fileScanner) throws InterruptedException, InvocationTargetException;

}
