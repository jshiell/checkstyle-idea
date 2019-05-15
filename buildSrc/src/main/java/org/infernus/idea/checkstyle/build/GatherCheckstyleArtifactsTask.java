package org.infernus.idea.checkstyle.build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.language.base.plugins.LifecycleBasePlugin;


/**
 * Download all supported versions of Checkstyle along with their transitive dependencies, for bundling with the
 * plugin.
 */
public class GatherCheckstyleArtifactsTask
    extends DefaultTask
{
    public static final String NAME = "gatherCheckstyleArtifacts";

    private final CheckstyleVersions csVersions;

    @OutputDirectory
    private final File bundledJarsDir;

    @OutputFile
    private final File classPathsInfoFile;


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
        classPathsInfoFile = new File(project.getBuildDir(), "resources-generated/checkstyle-classpaths.properties");
    }


    @TaskAction
    public void runTask() {
        final Set<File> bundledFiles = new TreeSet<>();
        final Properties classPaths = new SortedProperties();

        for (final String csVersion : csVersions.getVersions()) {
            final Set<File> files = resolveDependencies(getProject(), csVersion);
            classPaths.setProperty(csVersion, convertToClassPath(files));
            bundledFiles.addAll(files);
        }
        copyFiles(bundledFiles);
        createClassPathsFile(classPaths);
    }


    private Set<File> resolveDependencies(final Project project, final String checkstyleVersion) {
        final Dependency csDep = CheckstyleVersions.createCheckstyleDependency(project, checkstyleVersion);
        final Configuration csConf = project.getConfigurations().detachedConfiguration(csDep);
        return csConf.resolve();
    }

    private String convertToClassPath(final Set<File> resolvedDependecy) {
        final StringBuilder sb = new StringBuilder();
        for (final File f : resolvedDependecy) {
            sb.append(GradlePluginMain.CSLIB_TARGET_SUBFOLDER);
            sb.append('/');
            sb.append(f.getName());
            sb.append(';');
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }



    private void copyFiles(final Set<File> bundledJars) {
        for (final File bundledJar : bundledJars) {
            try {
                FileUtils.copyFileToDirectory(bundledJar, bundledJarsDir, true);
            }
            catch (IOException e) {
                throw new GradleException("Unable to copy file: " + bundledJar.getAbsolutePath(), e);
            }
        }
    }


    private void createClassPathsFile(final Properties classPaths) {
        //noinspection ResultOfMethodCallIgnored
        classPathsInfoFile.getParentFile().mkdir();

        try (OutputStream os = new FileOutputStream(classPathsInfoFile)) {
            classPaths.store(os, " Class path information for Checkstyle artifacts bundled with Checkstyle_IDEA");
        }
        catch (IOException e) {
            throw new GradleException("Unable to write classpath info file: " + classPathsInfoFile.getAbsolutePath(),
                e);
        }
    }


    public File getBundledJarsDir() {
        return bundledJarsDir;
    }

    public File getClassPathsInfoFile() {
        return classPathsInfoFile;
    }
}
