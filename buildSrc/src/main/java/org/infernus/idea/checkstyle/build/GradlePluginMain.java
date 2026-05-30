package org.infernus.idea.checkstyle.build;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.logging.TestExceptionFormat;
import org.gradle.api.tasks.testing.logging.TestLogEvent;
import org.gradle.api.tasks.testing.logging.TestLoggingContainer;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

import java.util.Collections;

/**
 * The main plugin class. The action starts here.
 */
public class GradlePluginMain implements Plugin<Project> {
    public static final String CSLIB_TARGET_SUBFOLDER = "checkstyle/lib";
    private static final String CSCLASSES_TARGET_SUBFOLDER = "checkstyle/classes";

    private CheckstyleVersions supportedCsVersions = null;

    @Override
    public void apply(final Project project) {
        Test testTask = (Test) project.getTasks().getByName(JavaPlugin.TEST_TASK_NAME);
        configureTestTask(testTask);

        establishSourceSets(project);
        readSupportedCheckstyleVersions(project);
        createCsAccessTestTask(project);
        createCrossCheckTasks(project);
        createCheckstyleArtifactTasks(project);
        new CustomSourceSetCreator(project).setupCoverageVerification();

        addCheckstyleFilesToSandbox(project, false);
        addCheckstyleFilesToSandbox(project, true);
    }

    private void establishSourceSets(final Project project) {
        final CustomSourceSetCreator sourceSetFactory = new CustomSourceSetCreator(project);
        sourceSetFactory.establishCsAccessSourceSet();
        sourceSetFactory.establishCsAccessTestSourceSet();
    }

    private void readSupportedCheckstyleVersions(final Project project) {
        supportedCsVersions = new CheckstyleVersions(project);
        project.getExtensions().getExtraProperties().set("supportedCsVersions", supportedCsVersions);
    }

    static void configureTestTask(final Test testTask) {
        testTask.testLogging((TestLoggingContainer tlc) -> {
            tlc.setEvents(Collections.singleton(TestLogEvent.FAILED));
            tlc.setShowStackTraces(true);
            tlc.setShowExceptions(true);
            tlc.setShowCauses(true);
            tlc.setExceptionFormat(TestExceptionFormat.FULL);
        });
        testTask.addTestListener(new TestSuiteStatsReporter(testTask.getLogger()));
    }

    private void createCsAccessTestTask(final Project project) {
        TaskProvider<CsaccessTestTask> provider = project.getTasks().register(CsaccessTestTask.NAME,
                CsaccessTestTask.class);
        provider.configure((CsaccessTestTask rct) -> {
            rct.setCheckstyleVersion(supportedCsVersions.getBaseVersion(), true);
            project.getTasks().getByName(JavaPlugin.TEST_TASK_NAME).dependsOn(rct);
        });
    }

    private void createCrossCheckTasks(final Project project) {
        final TaskContainer tasks = project.getTasks();

        TaskProvider<Task> provider = tasks.register(CsaccessTestTask.XTEST_TASK_NAME);
        provider.configure((Task xtestTask) -> {
            xtestTask.setGroup(CsaccessTestTask.XTEST_GROUP_NAME);
            xtestTask.setDescription("Runs the '" + CustomSourceSetCreator.CSACCESSTEST_SOURCESET_NAME + "' unit "
                    + "tests against all supported Checkstyle runtimes.");
            tasks.getByName(LifecycleBasePlugin.CHECK_TASK_NAME).dependsOn(xtestTask);
        });

        supportedCsVersions.getVersions().forEach((final String csVersion) -> {
            if (!supportedCsVersions.getBaseVersion().equals(csVersion)) {
                TaskProvider<CsaccessTestTask> xtProvider = tasks.register(CsaccessTestTask.getTaskName(csVersion),
                        CsaccessTestTask.class);
                xtProvider.configure((CsaccessTestTask xt) -> {
                    xt.setCheckstyleVersion(csVersion, false);
                    tasks.getByName(CsaccessTestTask.XTEST_TASK_NAME).dependsOn(xt);
                });
            }
        });
    }

    private void createCheckstyleArtifactTasks(final Project project) {
        TaskProvider<GatherCheckstyleArtifactsTask> taskProvider =
                project.getTasks().register(GatherCheckstyleArtifactsTask.NAME, GatherCheckstyleArtifactsTask.class);
        taskProvider.configure((GatherCheckstyleArtifactsTask task) -> {
            project.getTasks().getByName(JavaPlugin.PROCESS_RESOURCES_TASK_NAME).dependsOn(task);

            // Add generated classpath info file to resources
            SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
            if (sourceSets != null) {
                SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
                mainSourceSet.getResources().srcDir(task.getClassPathsInfoFile().getParentFile());
            }
        });
    }

    private void addCheckstyleFilesToSandbox(final Project project, final boolean test) {
        final TaskContainer tasks = project.getTasks();
        final String prepareTaskName;
        if (test) {
            prepareTaskName = "prepareTestSandbox";
        } else {
            prepareTaskName = "prepareSandbox";
        }

        final TaskProvider<Sync> prepareSandbox = tasks.named(prepareTaskName, Sync.class);
        final TaskProvider<GatherCheckstyleArtifactsTask> gatherTask =
                tasks.named(GatherCheckstyleArtifactsTask.NAME, GatherCheckstyleArtifactsTask.class);


        final JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
        final SourceSet csaccessSourceSet =
                java.getSourceSets().getByName(CustomSourceSetCreator.CSACCESS_SOURCESET_NAME);

        tasks.named("processResources").configure(task -> task.dependsOn(gatherTask));

        prepareSandbox.configure(task -> {
            task.dependsOn(gatherTask);
            task.dependsOn(tasks.named(csaccessSourceSet.getClassesTaskName()));

            task.from(gatherTask.map(GatherCheckstyleArtifactsTask::getBundledJarsDir), spec ->
                    spec.into("checkstyle-idea/" + CSLIB_TARGET_SUBFOLDER));

            task.from(csaccessSourceSet.getOutput(), spec ->
                    spec.into("checkstyle-idea/" + CSCLASSES_TARGET_SUBFOLDER));
        });
    }
}
