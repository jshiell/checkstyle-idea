package org.infernus.idea.checkstyle.build;

import java.io.File;

import groovy.lang.Closure;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Copy;


public class CopyCheckstyleArtifactsToSandboxTask
    extends Copy
{
    static final String TARGET_SUBFOLDER = "checkstyle/lib";

    public CopyCheckstyleArtifactsToSandboxTask() {
        setGroup("intellij");
        configureTask(false);
        final GatherCheckstyleArtifactsTask gatherTask = (GatherCheckstyleArtifactsTask) getProject().getTasks()
                .getByName(GatherCheckstyleArtifactsTask.NAME);
        dependsOn(gatherTask);
        from(gatherTask.getBundledJarsDir());
    }


    private void configureTask(final boolean pIsTest) {
        setDescription("Adds the gathered Checkstyle artifacts to the prepared " + (pIsTest ? "test " : "") +
                "sandbox");
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
                project.getTasks().getByName(RunCsaccessTestsTask.NAME).dependsOn(getOwner());
                return null;
            }
        });
    }
}
