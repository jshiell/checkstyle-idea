package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.util.ModulePaths;
import org.infernus.idea.checkstyle.util.TempDirProvider;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.*;


public class ModuleClassPathBuilder
{

    private static final Logger LOG = Logger.getInstance(ModuleClassPathBuilder.class);


    public ClassLoader build(final Module baseModule) {

        if (baseModule == null) {
            return getClass().getClassLoader();
        }

        final Project project = baseModule.getProject();
        final List<URL> outputPaths = new ArrayList<>();

        final Set<Module> transitiveDependencies = new HashSet<>();
        ModuleUtil.getDependencies(baseModule, transitiveDependencies);
        for (Module moduleInScope : transitiveDependencies) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Adding module to classpath: " + moduleInScope.getName());
            }
            outputPaths.addAll(ModulePaths.compilerOutputPathsFor(moduleInScope));
            outputPaths.addAll(ModulePaths.libraryPathsFor(moduleInScope));
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating class-loader with URLs: " + outputPaths);
        }

        final Optional<File> tempDir = new TempDirProvider().forCopiedLibraries(project);
        // TODO only stabilize when enabled in the options (default on Windows)
        final URL[] stabilizedCp = tempDir.map(t -> new ClasspathStabilizer(project, Paths.get(t.toURI())).stabilize
                (outputPaths)).orElse(outputPaths.toArray(new URL[outputPaths.size()]));
        return new URLClassLoader(stabilizedCp, getClass().getClassLoader());
    }
}
