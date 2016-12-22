package org.infernus.idea.checkstyle.build;

import groovy.lang.Closure;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.testing.Test;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

/**
 * Execute the unit tests contained in the 'csaccesTest' source set.
 */
public class RunCsaccessTestsTask
        extends Test
{
    public static final String NAME = "runCsaccessTests";


    public RunCsaccessTestsTask() {

        final Project project = getProject();
        setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
        setDescription("Runs the '" + CustomSourceSetCreator.CSACCESSTEST_SOURCESET_NAME + "' unit tests.");

        final JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
        final SourceSet csaccessTestSourceSet = javaConvention.getSourceSets().getByName(CustomSourceSetCreator
                .CSACCESSTEST_SOURCESET_NAME);
        setTestClassesDir(csaccessTestSourceSet.getOutput().getClassesDir());
        setClasspath(csaccessTestSourceSet.getRuntimeClasspath());

        configure((Closure) project.getProperties().get("testConfigClosure"));

        // The 'test' task now depends on this one final Project project = getProject();
        project.afterEvaluate(new Closure(this)
        {
            @Override
            public Void call(final Object... args) {
                project.getTasks().getByName(JavaPlugin.TEST_TASK_NAME).dependsOn(getOwner());
                return null;
            }
        });
    }
}
