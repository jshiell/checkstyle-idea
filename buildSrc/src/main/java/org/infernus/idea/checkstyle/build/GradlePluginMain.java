package org.infernus.idea.checkstyle.build;

import java.util.Collections;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.logging.TestExceptionFormat;
import org.gradle.api.tasks.testing.logging.TestLogEvent;
import org.gradle.api.tasks.testing.logging.TestLoggingContainer;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

/**
 * The main plugin class. The action starts here.
 */
public class GradlePluginMain
    implements Plugin<Project>
{
    private CheckstyleVersions supportedCsVersions = null;


    @Override
    @SuppressWarnings("NullableProblems")
    public void apply(final Project pProject) {
        establishSourceSets(pProject);
        readSupportedCheckstyleVersions(pProject);
        configureTestTask((Test) pProject.getTasks().getByName(JavaPlugin.TEST_TASK_NAME));
        createCsAccessTestTask(pProject);
        createCrossCheckTasks(pProject);
        new CustomSourceSetCreator(pProject).setupCoverageVerification();
        createCheckstyleArtifactTasks(pProject);
        wireIntellijPluginTasks(pProject);
    }


    private void establishSourceSets(final Project pProject) {
        final CustomSourceSetCreator sourceSetFactory = new CustomSourceSetCreator(pProject);
        sourceSetFactory.establishCsAccessSourceSet();
        sourceSetFactory.establishCsAccessTestSourceSet();
    }


    private void readSupportedCheckstyleVersions(final Project pProject) {
        supportedCsVersions = new CheckstyleVersions(pProject);
        pProject.getExtensions().getExtraProperties().set("supportedCsVersions", supportedCsVersions);
    }


    static void configureTestTask(final Test pTestTask) {
        pTestTask.testLogging((TestLoggingContainer tlc) -> {
            tlc.setEvents(Collections.singleton(TestLogEvent.FAILED));
            tlc.setShowStackTraces(true);
            tlc.setShowExceptions(true);
            tlc.setShowCauses(true);
            //tlc.setShowStandardStreams(true);
            tlc.setExceptionFormat(TestExceptionFormat.FULL);
        });
        pTestTask.addTestListener(new TestSuiteStatsReporter(pTestTask.getLogger()));
    }


    private void createCsAccessTestTask(final Project pProject) {
        TaskProvider<CsaccessTestTask> provider = pProject.getTasks().register(CsaccessTestTask.NAME,
                CsaccessTestTask.class);
        provider.configure((CsaccessTestTask rct) -> {
            rct.setCheckstyleVersion(supportedCsVersions.getBaseVersion(), true);
            pProject.getTasks().getByName(JavaPlugin.TEST_TASK_NAME).dependsOn(rct);
        });
    }


    private void createCrossCheckTasks(final Project pProject) {
        final TaskContainer tasks = pProject.getTasks();

        TaskProvider<Task> provider = tasks.register(CsaccessTestTask.XTEST_TASK_NAME);
        provider.configure((Task xtestTask) -> {
            xtestTask.setGroup(CsaccessTestTask.XTEST_GROUP_NAME);
            xtestTask.setDescription("Runs the '" + CustomSourceSetCreator.CSACCESSTEST_SOURCESET_NAME + "' unit " +
                    "tests against all supported Checkstyle runtimes.");
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


    private void createCheckstyleArtifactTasks(final Project pProject) {
        final TaskContainer tasks = pProject.getTasks();
        tasks.create(GatherCheckstyleArtifactsTask.NAME, GatherCheckstyleArtifactsTask.class);

        tasks.create("copyCheckstyleArtifactsToSandbox", CopyCheckstyleArtifactsToSandboxTask.class);
        TaskProvider<CopyCheckstyleArtifactsToSandboxTask> copyProvider1 = tasks.register(
                "copyCheckstyleArtifactsToTestSandbox", CopyCheckstyleArtifactsToSandboxTask.class);
        copyProvider1.configure(CopyCheckstyleArtifactsToSandboxTask::setTest);

        tasks.create("copyClassesToSandbox", CopyClassesToSandboxTask.class);
        TaskProvider<CopyClassesToSandboxTask> copyProvider2 = tasks.register("copyClassesToTestSandbox",
                CopyClassesToSandboxTask.class);
        copyProvider2.configure(CopyClassesToSandboxTask::setTest);
    }


    /**
     * Defer some of the wiring until after the intellij plugin's tasks have been created.
     *
     * @param pProject the Gradle project
     */
    private void wireIntellijPluginTasks(final Project pProject) {
        final TaskContainer tasks = pProject.getTasks();
        tasks.all((Task task) -> {
            if ("buildPlugin".equals(task.getName()) || "runIdea".equals(task.getName()) || "runIde".equals(task.getName())) {
                task.dependsOn(tasks.getByName("copyCheckstyleArtifactsToSandbox"));
                task.dependsOn(tasks.getByName("copyClassesToSandbox"));
            } else if ("prepareSandbox".equals(task.getName())) {
                tasks.getByName("copyCheckstyleArtifactsToSandbox").dependsOn(task);
                tasks.getByName("copyClassesToSandbox").dependsOn(task);
            } else if ("prepareTestsSandbox".equals(task.getName())) {
                tasks.getByName("copyCheckstyleArtifactsToTestSandbox").dependsOn(task);
                tasks.getByName("copyClassesToTestSandbox").dependsOn(task);
            }
        });
    }
}
