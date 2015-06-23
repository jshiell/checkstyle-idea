package org.infernus.idea.checkstyle.util;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.impl.jar.JarFileSystemImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStyleConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

public class ModuleClassPathBuilder {

    private static final Log LOG = LogFactory.getLog(ModuleClassPathBuilder.class);

    private final CheckStyleConfiguration configuration;
    private final ReadWriteLock thirdPartyClassLoaderLock = new ReentrantReadWriteLock();

    private ClassLoader thirdPartyClassLoader;

    public ModuleClassPathBuilder(@NotNull final CheckStyleConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Build a class loader for the compilation path of the module.
     *
     * @param baseModule the module in question.
     * @return the class loader to use, or null if the module was null.
     * @throws java.net.MalformedURLException if the URL conversion fails.
     */
    public ClassLoader build(final Module baseModule)
            throws MalformedURLException {

        if (baseModule == null) {
            return thirdPartyClassLoader();
        }

        final Set<URL> outputPaths = new HashSet<URL>();

        final Set<Module> transitiveDependencies = new HashSet<Module>();
        ModuleUtil.getDependencies(baseModule, transitiveDependencies);
        for (Module moduleInScope : transitiveDependencies) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Adding module to classpath: " + moduleInScope.getName());
            }
            outputPaths.addAll(compilerOutputPathsFor(moduleInScope));
            outputPaths.addAll(pathsOf(libraryRootsFor(moduleInScope)));
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
        final URL[] urlList = new URL[thirdPartyClasses.size()];
        int index = 0;
        for (final String pathElement : thirdPartyClasses) {
            try {
                // toURI().toURL() escapes, whereas toURL() doesn't.
                urlList[index] = urlFor(pathElement);
                ++index;

            } catch (MalformedURLException e) {
                LOG.error("Third party classpath element is malformed: " + pathElement, e);
            }
        }
        return urlList;
    }

    private VirtualFile[] libraryRootsFor(final Module module) {
        return LibraryUtil.getLibraryRoots(new Module[]{module}, false, false);
    }

    private List<URL> compilerOutputPathsFor(final Module module) throws MalformedURLException {
        final CompilerModuleExtension compilerModule = CompilerModuleExtension.getInstance(module);
        if (compilerModule != null) {
            return pathsOf(compilerModule.getOutputRoots(true));
        }
        return emptyList();
    }

    private List<URL> pathsOf(final VirtualFile[] files) throws MalformedURLException {
        final List<URL> outputPaths = new ArrayList<>();
        for (final VirtualFile file : files) {
            outputPaths.add(urlFor(pathOf(file)));
        }
        return outputPaths;
    }

    @NotNull
    private URL urlFor(final String filePath) throws MalformedURLException {
        return new File(filePath).toURI().toURL();
    }

    @NotNull
    private String pathOf(final VirtualFile file) {
        String filePath = stripJarFileSuffix(file);
        if (filePath.endsWith(".jar")) {
            return mirrorPathOf(file).orElse(filePath);
        }
        return filePath;
    }

    @NotNull
    private Optional<String> mirrorPathOf(final VirtualFile file) {
        final JarFileSystem jarFileSystem = JarFileSystem.getInstance();
        if (jarFileSystem instanceof JarFileSystemImpl) {
            final File mirroredFile = ((JarFileSystemImpl) jarFileSystem).getMirroredFile(file);
            if (mirroredFile != null) {
                return Optional.of(mirroredFile.getPath());
            }
        }
        return empty();
    }

    @NotNull
    private String stripJarFileSuffix(final VirtualFile file) {
        final String filePath = file.getPath();
        if (filePath.endsWith("!/")) { // filter JAR suffix
            return filePath.substring(0, filePath.length() - 2);
        }
        return filePath;
    }
}
