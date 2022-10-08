package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.util.ModulePaths;
import org.infernus.idea.checkstyle.util.TempDirProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class ModuleClassPathBuilder {
    private static final Logger LOG = Logger.getInstance(ModuleClassPathBuilder.class);

    private final Project project;

    public ModuleClassPathBuilder(@NotNull final Project project) {
        this.project = project;
    }

    public ClassLoader build(final Module baseModule) {
        if (baseModule == null) {
            return getClass().getClassLoader();
        }

        final Project baseModuleProject = baseModule.getProject();
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

        URL[] effectiveClasspath = outputPaths.toArray(new URL[0]);
        if (wantsCopyLibs()) {
            final Optional<File> tempDir = new TempDirProvider().forCopiedLibraries(baseModuleProject);
            if (tempDir.isPresent()) {
                final Path t = Paths.get(tempDir.get().toURI());
                effectiveClasspath = new ClasspathStabilizer(baseModuleProject, t).stabilize(outputPaths);
            }
        }
        return new ResourceFilteringURLClassLoader(effectiveClasspath, getClass().getClassLoader(), "commons-logging.properties");
    }

    private PluginConfigurationManager pluginConfigurationManager() {
        return project.getService(PluginConfigurationManager.class);
    }

    private boolean wantsCopyLibs() {
        return pluginConfigurationManager().getCurrent().isCopyLibs();
    }
}
