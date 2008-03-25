package org.infernus.idea.checkstyle;

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

/**
 * Abstract CheckerThread.
 */
public abstract class AbstractCheckerThread extends Thread {

    /**
     * Files to scan.
     */
    protected final List<PsiFile> files = new ArrayList<PsiFile>();

    /**
     * Map files to modules.
     */
    protected final Map<PsiFile, Module> fileToModuleMap
            = new HashMap<PsiFile, Module>();

    /**
     * Scan results.
     */
    protected Map<PsiFile, List<ProblemDescriptor>> fileResults;

    /**
     * Reference to plugin
     */
    protected CheckStylePlugin plugin;

    public AbstractCheckerThread(CheckStylePlugin checkStylePlugin, final List<VirtualFile> virtualFiles) {
        this.plugin = checkStylePlugin;
        
        if (files == null) {
            throw new IllegalArgumentException("Files may not be null.");
        }
        // this needs to be done on the main thread.
        final PsiManager psiManager = PsiManager.getInstance(this.plugin.project);
        for (final VirtualFile virtualFile : virtualFiles) {
            final PsiFile psiFile = psiManager.findFile(virtualFile);
            if (psiFile != null) {
                files.add(psiFile);
            }
        }

        // build module map (also on main frame)
        for (final PsiFile file : files) {
            this.fileToModuleMap.put(file, ModuleUtil.findModuleForPsiElement(file));
        }
    }

    protected void processFilesForModuleInfoAndScan() throws Throwable {
        final Map<Module, ClassLoader> moduleClassLoaderMap
                = new HashMap<Module, ClassLoader>();


        for (final PsiFile psiFile : files) {
            if (psiFile == null) {
                continue;
            }

            final Module module = fileToModuleMap.get(psiFile);
            final ClassLoader moduleClassLoader;
            if (moduleClassLoaderMap.containsKey(module)) {
                moduleClassLoader = moduleClassLoaderMap.get(module);
            } else {
                moduleClassLoader = AbstractCheckerThread.this.plugin.buildModuleClassLoader(module);
                moduleClassLoaderMap.put(module, moduleClassLoader);
            }

            final FileScanner fileScanner = new FileScanner(this.plugin,
                    psiFile, moduleClassLoader);
            
            this.runFileScanner(fileScanner);

            // check for errors
            if (fileScanner.getError() != null) {
                // throw any exceptions from the thread
                throw fileScanner.getError();
            }

            // add results if necessary
            if (fileScanner.getResults() != null
                    && fileScanner.getResults().size() > 0) {
                fileResults.put(psiFile, fileScanner.getResults());
            }
        }
    }

    public abstract void runFileScanner(FileScanner fileScanner) throws InterruptedException, InvocationTargetException;

}
