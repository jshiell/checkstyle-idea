package org.infernus.idea.checkstyle.build;

import java.io.File;

import groovy.lang.Closure;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;

/**
 * Make the compiled classes and resources from the 'csaccess' sourceset available to the plugin by copying it to the
 * sandbox. Test code not affected.
 */
public class CopyClassesToSandboxTask
        extends Copy
{
    private static final String CHECKSTYLE_SOURCESET_NAME = "csaccess";

    private static final String TARGET_SUBFOLDER = "checkstyle/classes";


    public CopyClassesToSandboxTask() {
        setGroup("intellij");
        dependsOn(getProject().getTasks().getByName(CHECKSTYLE_SOURCESET_NAME + "Classes"));
        JavaPluginConvention javaConvention = getProject().getConvention().getPlugin(JavaPluginConvention.class);
        SourceSet csaccessSourceSet = javaConvention.getSourceSets().getByName(CHECKSTYLE_SOURCESET_NAME);
        from(csaccessSourceSet.getOutput());
        configureTask(false);
    }


    private void configureTask(final boolean pIsTest) {
        setDescription("Copy classes from \'" + CHECKSTYLE_SOURCESET_NAME + "\' sourceset into the prepared " +
                (pIsTest ? "test " : "") + "sandbox");
        into(new File(getProject().getBuildDir(), "idea-sandbox/plugins" + (pIsTest ? "-test" : "") +
                "/CheckStyle-IDEA/" + TARGET_SUBFOLDER));
    }


    public void setTest() {
        configureTask(true);
        final Project project = getProject();

        // The 'test' task now depends on this one
        project.afterEvaluate(new Closure(this)
        {
            @Override
            public Void call(final Object... args) {
                project.getTasks().getByName(JavaPlugin.TEST_TASK_NAME).dependsOn(getOwner());
                project.getTasks().getByName("runCsaccessTests").dependsOn(getOwner());
                return null;
            }
        });
    }
}
