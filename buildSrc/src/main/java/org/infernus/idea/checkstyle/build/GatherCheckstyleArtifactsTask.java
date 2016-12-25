package org.infernus.idea.checkstyle.build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import groovy.lang.Closure;
import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.language.base.plugins.LifecycleBasePlugin;


/**
 * Download all supported versions of Checkstyle along with their transitive dependencies, for bundling with the
 * plugin.
 */
public class GatherCheckstyleArtifactsTask
    extends DefaultTask
{
    public static final String NAME = "gatherCheckstyleArtifacts";

    private final File bundledJarsDir;

    private final File classPathsInfoFile;



    public GatherCheckstyleArtifactsTask()
    {
        super();
        setGroup(LifecycleBasePlugin.BUILD_GROUP);
        setDescription("Gathers Checkstyle libraries and their dependencies for bundling");
        final Project project = getProject();

        // Task Inputs: the property file with the list of supported Checkstyle versions
        final CheckstyleVersions csVersions = new CheckstyleVersions(project);
        getInputs().file(csVersions.getPropertyFile());

        // Task Outputs: the directory full of JARs, and the classpath info file
        bundledJarsDir = getTemporaryDir();
        classPathsInfoFile = new File(project.getBuildDir(), "resources-generated/checkstyle-classpaths.properties");
        getOutputs().dir(bundledJarsDir);
        getOutputs().file(classPathsInfoFile);

        // Add generated classpath info file to resources
        SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
        SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        mainSourceSet.getResources().srcDir(classPathsInfoFile.getParentFile());

        // 'processResources' now depends on this task
        project.afterEvaluate(new Closure<Void>(this)
        {
            @Override
            public Void call(final Object... args)
            {
                project.getTasks().getByName(JavaPlugin.PROCESS_RESOURCES_TASK_NAME).dependsOn(getOwner());
                return null;
            }
        });

        doLast(new Closure<Void>(this)
        {
            @Override
            public Void call()
            {
                final Set<File> bundledFiles = new TreeSet<>();
                final Properties classPaths = new SortedProperties();

                for (final String csVersion : csVersions.getVersions()) {
                    final Set<File> files = resolveDependencies(project, csVersion);
                    classPaths.setProperty(csVersion, convertToClassPath(files));
                    bundledFiles.addAll(files);
                }
                copyFiles(bundledFiles);
                createClassPathsFile(classPaths);
                return null;
            }
        });
    }



    private Set<File> resolveDependencies(final Project pProject, final String pCheckstyleVersion)
    {
        final Dependency csDep = CheckstyleVersions.createCheckstyleDependency(pProject, pCheckstyleVersion);
        final Configuration csConf = pProject.getConfigurations().detachedConfiguration(csDep);
        final Set<File> files = csConf.resolve();
        return files;
    }



    private String convertToClassPath(final Set<File> pResolvedDependecy)
    {
        final StringBuilder sb = new StringBuilder();
        for (final File f : pResolvedDependecy) {
            sb.append(CopyCheckstyleArtifactsToSandboxTask.TARGET_SUBFOLDER);
            sb.append('/');
            sb.append(f.getName());
            sb.append(';');
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }



    private void copyFiles(final Set<File> pBundledJars)
    {
        for (final File bundledJar : pBundledJars) {
            try {
                FileUtils.copyFileToDirectory(bundledJar, bundledJarsDir, true);
            }
            catch (IOException e) {
                throw new GradleException("Unable to copy file: " + bundledJar.getAbsolutePath(), e);
            }
        }
    }



    private void createClassPathsFile(final Properties pClassPaths)
    {
        //noinspection ResultOfMethodCallIgnored
        classPathsInfoFile.getParentFile().mkdir();

        try (
            final OutputStream os = new FileOutputStream(classPathsInfoFile)
        ) {
            pClassPaths.store(os, " Class path information for Checkstyle artifacts bundled with Checkstyle_IDEA");
        }
        catch (IOException e) {
            throw new GradleException("Unable to write classpath info file: " + classPathsInfoFile.getAbsolutePath(),
                e);
        }
    }



    public File getBundledJarsDir()
    {
        return bundledJarsDir;
    }
}
