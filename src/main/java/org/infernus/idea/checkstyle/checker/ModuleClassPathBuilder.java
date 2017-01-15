package org.infernus.idea.checkstyle.checker;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.util.ModulePaths;


public class ModuleClassPathBuilder
{
    private static final Log LOG = LogFactory.getLog(ModuleClassPathBuilder.class);


    public ClassLoader build(final Module baseModule) {

        if (baseModule == null) {
            return getClass().getClassLoader();
        }

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

        return new URLClassLoader(outputPaths.toArray(new URL[outputPaths.size()]), getClass().getClassLoader());
    }
}
