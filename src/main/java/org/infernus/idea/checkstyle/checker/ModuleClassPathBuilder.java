package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStyleConfiguration;
import org.infernus.idea.checkstyle.util.ModulePaths;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ModuleClassPathBuilder {

    private static final Log LOG = LogFactory.getLog(ModuleClassPathBuilder.class);

    private final CheckStyleConfiguration configuration;
    private final ReadWriteLock thirdPartyClassLoaderLock = new ReentrantReadWriteLock();

    private ClassLoader thirdPartyClassLoader;

    public ModuleClassPathBuilder(@NotNull final CheckStyleConfiguration configuration) {
        this.configuration = configuration;
    }

    public ClassLoader build(final Module baseModule) {

        if (baseModule == null) {
            return thirdPartyClassLoader();
        }

        final Set<URL> outputPaths = new HashSet<>();

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

        return new ClassOnlyClassLoader(outputPaths.toArray(new URL[outputPaths.size()]), thirdPartyClassLoader());
    }

    /**
     * Force a reload of any cached classpath information.
     */
    public void reset() {
        thirdPartyClassLoaderLock.writeLock().lock();
        try {
            thirdPartyClassLoader = null; // reset to force reload
        } finally {
            thirdPartyClassLoaderLock.writeLock().unlock();
        }
    }

    private ClassLoader thirdPartyClassLoader() {
        thirdPartyClassLoaderLock.readLock().lock();
        try {
            if (thirdPartyClassLoader == null) {
                initialiseThirdPartyClassLoader();
            }

            return thirdPartyClassLoader;

        } finally {
            thirdPartyClassLoaderLock.readLock().unlock();
        }
    }

    private void initialiseThirdPartyClassLoader() {
        thirdPartyClassLoaderLock.readLock().unlock();
        thirdPartyClassLoaderLock.writeLock().lock();

        try {
            if (thirdPartyClassLoader == null) {
                final List<String> thirdPartyClasses = configuration.getThirdPartyClassPath();
                if (!thirdPartyClasses.isEmpty()) {
                    thirdPartyClassLoader = new URLClassLoader(listUrlsOf(thirdPartyClasses), getClass().getClassLoader());

                } else {
                    thirdPartyClassLoader = getClass().getClassLoader();
                }
            }

        } finally {
            thirdPartyClassLoaderLock.writeLock().unlock();
            thirdPartyClassLoaderLock.readLock().lock();
        }
    }

    @NotNull
    private URL[] listUrlsOf(final List<String> thirdPartyClasses) {
        final List<URL> urlList = new ArrayList<>(thirdPartyClasses.size());
        for (final String pathElement : thirdPartyClasses) {
            try {
                urlList.add(urlFor(pathElement));
            } catch (MalformedURLException e) {
                LOG.error("Third party classpath element is malformed: " + pathElement, e);
            }
        }
        return urlList.toArray(new URL[urlList.size()]);
    }

    @NotNull
    private static URL urlFor(final String filePath) throws MalformedURLException {
        // toURI().toURL() escapes, whereas toURL() doesn't.
        return new File(filePath).toURI().toURL();
    }

}
