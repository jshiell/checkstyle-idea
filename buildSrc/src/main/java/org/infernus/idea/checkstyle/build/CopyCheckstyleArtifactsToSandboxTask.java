package org.infernus.idea.checkstyle.build;

import java.io.File;

import groovy.lang.Closure;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Copy;


public class CopyCheckstyleArtifactsToSandboxTask extends Copy {

    static final String TARGET_SUBFOLDER = "lib";

    public CopyCheckstyleArtifactsToSandboxTask() {
        super();
        setGroup("intellij");
        configureTask(false);
        final GatherCheckstyleArtifactsTask gatherTask = (GatherCheckstyleArtifactsTask) getProject().getTasks()
                .getByName(GatherCheckstyleArtifactsTask.NAME);
        dependsOn(gatherTask);
        dependsOn(getProject().getTasks().getByName("prepareTestingSandbox"));
        from(gatherTask.getBundledJarsDir());
    }


    private void configureTask(final boolean test) {
        setDescription("Adds the gathered Checkstyle artifacts to the prepared " + (test ? "test " : "")
                + "sandbox");
        into(new File(getProject().getBuildDir(), "idea-sandbox/plugins" + (test ? "-test" : "")
                + "/CheckStyle-IDEA/" + TARGET_SUBFOLDER));
    }


    public void setTest() {
        configureTask(true);
        final Project project = getProject();

        // The 'test' task now depends on this one
        project.afterEvaluate(new Closure(this) {
            @Override
            public Void call(final Object... args) {
                project.getTasks().getByName(JavaPlugin.TEST_TASK_NAME).dependsOn(getOwner());
                project.getTasks().getByName(CsaccessTestTask.NAME).dependsOn(getOwner());
                return null;
            }
        });
    }
}
