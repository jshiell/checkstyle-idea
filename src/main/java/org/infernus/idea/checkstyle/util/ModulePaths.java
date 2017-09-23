package org.infernus.idea.checkstyle.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.impl.jar.JarFileSystemImpl;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;

public final class ModulePaths {

    private static final Logger LOG = Logger.getInstance(ModulePaths.class);

    private ModulePaths() {
        // utility class
    }

    public static List<URL> libraryPathsFor(final Module moduleInScope) {
        return pathsOf(libraryRootsFor(moduleInScope));
    }

    private static VirtualFile[] libraryRootsFor(final Module module) {
        return LibraryUtil.getLibraryRoots(new Module[]{module}, false, false);
    }

    public static List<URL> compilerOutputPathsFor(final Module module) {
        final CompilerModuleExtension compilerModule = CompilerModuleExtension.getInstance(module);
        if (compilerModule != null) {
            return pathsOf(compilerModule.getOutputRoots(true));
        }
        return emptyList();
    }

    private static List<URL> pathsOf(final VirtualFile[] files) {
        final List<URL> outputPaths = new ArrayList<>();
        for (final VirtualFile file : files) {
            try {
                outputPaths.add(urlFor(pathOf(file)));
            } catch (MalformedURLException e) {
                LOG.warn("Malformed virtual file URL: " + file, e);
            }
        }
        return outputPaths;
    }

    @NotNull
    private static URL urlFor(final String filePath) throws MalformedURLException {
        // toURI().toURL() escapes, whereas toURL() doesn't.
        return new File(filePath).toURI().toURL();
    }

    @NotNull
    private static String pathOf(final VirtualFile file) {
        String filePath = stripJarFileSuffix(file);
        if (filePath.endsWith(".jar")) {
            return mirrorPathOf(file).orElse(filePath);
        }
        return filePath;
    }

    @NotNull
    private static Optional<String> mirrorPathOf(final VirtualFile file) {
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
    private static String stripJarFileSuffix(final VirtualFile file) {
        final String filePath = file.getPath();
        if (filePath.endsWith("!/")) { // filter JAR suffix
            return filePath.substring(0, filePath.length() - 2);
        }
        return filePath;
    }
}
