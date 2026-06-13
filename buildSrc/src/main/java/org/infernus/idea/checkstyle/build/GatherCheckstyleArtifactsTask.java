package org.infernus.idea.checkstyle.build;

import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import static java.util.stream.Collectors.toSet;


/**
 * Download all supported versions of Checkstyle along with their transitive dependencies, for bundling with the
 * plugin.
 */
public class GatherCheckstyleArtifactsTask
        extends DefaultTask {
    public static final String NAME = "gatherCheckstyleArtifacts";

    private final Map<String, Set<File>> rawVersionsToDependencies = new HashMap<>();
    private final Map<String, String> nonBundledManifestEntries = new HashMap<>();
    private final CheckstyleVersions csVersions;

    @OutputDirectory
    private final File bundledJarsDir;

    @OutputFile
    private final File classPathsInfoFile;

    @OutputFile
    private final File downloadManifestFile;

    public GatherCheckstyleArtifactsTask() {
        super();
        setGroup(LifecycleBasePlugin.BUILD_GROUP);
        setDescription("Gathers Checkstyle libraries and their dependencies for bundling");
        final Project project = getProject();

        // Task Inputs: the property file with the list of supported Checkstyle versions
        csVersions = new CheckstyleVersions(project);
        getInputs().file(csVersions.getPropertyFile());

        // Task Outputs: the directory full of JARs, and the classpath info file
        bundledJarsDir = getTemporaryDir();
        File resourcesGenDir = new File(project.getLayout().getBuildDirectory().getAsFile().get(), "resources-generated");
        classPathsInfoFile = new File(resourcesGenDir, "checkstyle-classpaths.properties");
        downloadManifestFile = new File(resourcesGenDir, "checkstyle-download-manifest.properties");

        for (final String csVersion : csVersions.getBundledVersions()) {
            final Set<File> dependencies = resolveDependencies(project, csVersion);
            rawVersionsToDependencies.put(csVersion, dependencies);
        }

        SortedSet<String> nonBundledVersions = new TreeSet<>(csVersions.getVersions());
        nonBundledVersions.removeAll(csVersions.getBundledVersions());
        for (final String csVersion : nonBundledVersions) {
            nonBundledManifestEntries.put(csVersion, ManifestFormatter.buildEntry(resolveArtifacts(project, csVersion)));
        }
    }

    @TaskAction
    public void runTask() {
        final Set<File> bundledFiles = new TreeSet<>();
        final Properties classPaths = new SortedProperties();

        for (final String csVersion : csVersions.getBundledVersions()) {
            Set<File> dependencies = rawVersionsToDependencies.get(csVersion);
            dependencies.forEach(bundledFiles::add);
            classPaths.setProperty(csVersion, convertToClassPath(
                    dependencies.stream().map(File::getName).collect(toSet())));
        }

        copyFiles(bundledFiles);
        createClassPathsFile(classPaths);
        createDownloadManifestFile();
    }

    private Set<File> resolveDependencies(final Project project, final String checkstyleVersion) {
        final Dependency csDep = CheckstyleVersions.createCheckstyleDependency(project, checkstyleVersion);
        final Configuration csConf = project.getConfigurations().detachedConfiguration(csDep);
        CheckstyleVersions.applyGoogleCollectionsWorkaround(csConf);
        return csConf.resolve();
    }

    private Set<ResolvedArtifact> resolveArtifacts(final Project project, final String checkstyleVersion) {
        final Dependency csDep = CheckstyleVersions.createCheckstyleDependency(project, checkstyleVersion);
        final Configuration csConf = project.getConfigurations().detachedConfiguration(csDep);
        CheckstyleVersions.applyGoogleCollectionsWorkaround(csConf);
        return csConf.getResolvedConfiguration().getResolvedArtifacts();
    }

    private String convertToClassPath(final Collection<String> resolvedDependencies) {
        final StringBuilder sb = new StringBuilder();
        for (final String fileName : resolvedDependencies) {
            sb.append(GradlePluginMain.CSLIB_TARGET_SUBFOLDER);
            sb.append('/');
            sb.append(fileName);
            sb.append(';');
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private void copyFiles(final Set<File> bundledJars) {
        for (final File bundledJar : bundledJars) {
            try {
                FileUtils.copyFileToDirectory(bundledJar, bundledJarsDir, true);
            } catch (IOException e) {
                throw new GradleException("Unable to copy file: " + bundledJar.getAbsolutePath(), e);
            }
        }
    }

    private void createClassPathsFile(final Properties classPaths) {
        //noinspection ResultOfMethodCallIgnored
        classPathsInfoFile.getParentFile().mkdir();

        try (OutputStream os = new FileOutputStream(classPathsInfoFile)) {
            classPaths.store(os, " Class path information for Checkstyle artifacts bundled with Checkstyle_IDEA");
        } catch (IOException e) {
            throw new GradleException("Unable to write classpath info file: " + classPathsInfoFile.getAbsolutePath(),
                    e);
        }
    }

    private void createDownloadManifestFile() {
        //noinspection ResultOfMethodCallIgnored
        downloadManifestFile.getParentFile().mkdir();

        final Properties manifest = new SortedProperties();
        nonBundledManifestEntries.forEach(manifest::setProperty);

        try (OutputStream os = new FileOutputStream(downloadManifestFile)) {
            manifest.store(os, " Download manifest for non-bundled Checkstyle versions");
        } catch (IOException e) {
            throw new GradleException("Unable to write download manifest file: " + downloadManifestFile.getAbsolutePath(), e);
        }
    }

    public File getBundledJarsDir() {
        return bundledJarsDir;
    }

    public File getClassPathsInfoFile() {
        return classPathsInfoFile;
    }

    public File getDownloadManifestFile() {
        return downloadManifestFile;
    }
}
