package org.infernus.idea.checkstyle.build;

import java.io.File;

import groovy.lang.Closure;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.testing.Test;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.jetbrains.annotations.NotNull;


/**
 * Gradle task that runs the unit tests in 'csaccessTest' against one of the supported Checkstyle versions.
 */
public abstract class CsaccessTestTask extends Test {
    public static final String XTEST_GROUP_NAME = "xtest";
    public static final String XTEST_TASK_NAME = "xtest";

    public static final String NAME = "runCsaccessTests";

    public static final String CSVERSION_SYSPROP_NAME = "org.infernus.idea.checkstyle.version";

    private FileCollection effectiveClassPath = null;

    private Property<Boolean> dryRun;

    public CsaccessTestTask() {
        final Project project = getProject();
        final JavaPluginExtension jpc = project.getExtensions().getByType(JavaPluginExtension.class);
        final SourceSet csaccessTestSourceSet = jpc.getSourceSets().getByName(
                CustomSourceSetCreator.CSACCESSTEST_SOURCESET_NAME);

        dependsOn(project.getTasks().getByName(csaccessTestSourceSet.getClassesTaskName()));

        GradlePluginMain.configureTestTask(this);
        setTestClassesDirs(csaccessTestSourceSet.getOutput().getClassesDirs());
        setClasspath(csaccessTestSourceSet.getRuntimeClasspath()
                .plus(csaccessTestSourceSet.getCompileClasspath())); // TODO delete?
    }

    public static String getTaskName(final String pCheckstyleVersion) {
        return "xtest_" + CheckstyleVersions.toGradleVersion(pCheckstyleVersion);
    }

    public void setCheckstyleVersion(final String checkstyleVersion, final boolean isBaseVersion) {
        setDescription("Runs the '" + CustomSourceSetCreator.CSACCESSTEST_SOURCESET_NAME + "' unit tests against a "
                + "Checkstyle " + checkstyleVersion + " runtime.");
        getReports().getJunitXml().getRequired().set(false);
        if (isBaseVersion) {
            setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
            getReports().getHtml().getRequired().set(true);
        } else {
            setGroup(XTEST_GROUP_NAME);
            getReports().getHtml().getRequired().set(false);
        }

        // Make the Checkstyle version available to the test cases via a system property.
        configure(new Closure<Void>(this) {
            @Override
            public Void call() {
                systemProperty(CSVERSION_SYSPROP_NAME, checkstyleVersion);
                return null;
            }
        });

        effectiveClassPath = setClassPathForVersion(checkstyleVersion, getProject());
    }

    private @NotNull FileCollection setClassPathForVersion(final String checkstyleVersion, final Project project) {
        final JavaPluginExtension jpc = project.getExtensions().getByType(JavaPluginExtension.class);
        final Dependency csDep = CheckstyleVersions.createCheckstyleDependency(project, checkstyleVersion);
        final ConfigurationContainer configurations = project.getConfigurations();
        final Configuration detachedConfiguration = configurations.detachedConfiguration(csDep);
        // workaround for Checkstyle#14123
        detachedConfiguration
                .getResolutionStrategy()
                .getCapabilitiesResolution()
                .withCapability("com.google.collections", "google-collections", resolutionDetails -> resolutionDetails.select("com.google.guava:guava:0"));

        final SourceSetContainer sourceSets = jpc.getSourceSets();
        final SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        final SourceSet testSourceSet = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);
        final SourceSet csaccessSourceSet = sourceSets.getByName(CustomSourceSetCreator.CSACCESS_SOURCESET_NAME);
        final SourceSet csaccessTestSourceSet = jpc.getSourceSets().getByName(CustomSourceSetCreator.CSACCESSTEST_SOURCESET_NAME);

        return project.files(
                        csaccessTestSourceSet.getOutput().getResourcesDir(),
                        csaccessSourceSet.getOutput().getResourcesDir(),
                        mainSourceSet.getOutput().getResourcesDir())
                .plus(csaccessTestSourceSet.getOutput().getClassesDirs())
                .plus(csaccessSourceSet.getOutput().getClassesDirs())
                .plus(mainSourceSet.getOutput().getClassesDirs())
                .plus(project.files(detachedConfiguration.getFiles()))
                .plus(csaccessTestSourceSet.getRuntimeClasspath())
                .plus(csaccessTestSourceSet.getCompileClasspath())
                .minus(testSourceSet.getOutput().getClassesDirs())
                .minus(project.files(testSourceSet.getOutput().getResourcesDir()));
    }

    /**
     * Overriding getClasspath() in order to set the final classpath is an unusual solution, but it was the only
     * solution which included the classpath entries generated by the IntelliJ plugin creation plugin (which, in my
     * humble opinion, should be considered seriously broken).
     *
     * @return the classpath to use to execute the tests
     */
    @Override
    public @NotNull FileCollection getClasspath() {
        if (effectiveClassPath == null) {
            throw new IllegalStateException("setCheckstyleVersion has not been called");
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("--------------------------------------------------------------------------");
            getLogger().debug("Effective classpath of " + getName() + ":");
            for (File f : effectiveClassPath) {
                getLogger().debug("\t- " + f.getAbsolutePath());
            }
        }
        return effectiveClassPath;
    }

    @Override
    public @NotNull Property<Boolean> getDryRun() {
        if (dryRun == null) {
            dryRun = getObjectFactory().property(Boolean.class).value(false);
        }
        return dryRun;
    }


}
