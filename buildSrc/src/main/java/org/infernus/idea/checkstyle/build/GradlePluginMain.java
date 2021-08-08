package org.infernus.idea.checkstyle.build;

import java.io.File;
import java.util.Collections;
import java.util.function.Consumer;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
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
    implements Plugin<Project> {
    public static final String CSLIB_TARGET_SUBFOLDER = "checkstyle/lib";

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


    private void createCheckstyleArtifactTasks(final Project pProject) {
        TaskProvider<GatherCheckstyleArtifactsTask> taskProvider =
                pProject.getTasks().register(GatherCheckstyleArtifactsTask.NAME, GatherCheckstyleArtifactsTask.class);
        taskProvider.configure((GatherCheckstyleArtifactsTask task) -> {
            pProject.getTasks().getByName(JavaPlugin.PROCESS_RESOURCES_TASK_NAME).dependsOn(task);

            // Add generated classpath info file to resources
            SourceSetContainer sourceSets = (SourceSetContainer) pProject.getProperties().get("sourceSets");
            SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            mainSourceSet.getResources().srcDir(task.getClassPathsInfoFile().getParentFile());
        });

        createCopyCheckstyleArtifactsToSandboxTask(pProject, false);
        createCopyCheckstyleArtifactsToSandboxTask(pProject, true);

        createCopyClassesToSandboxTask(pProject, false);
        createCopyClassesToSandboxTask(pProject, true);
    }


    private void createCopyCheckstyleArtifactsToSandboxTask(final Project project, final boolean test) {
        final TaskContainer tasks = project.getTasks();
        final String taskName = test ? "copyCheckstyleArtifactsToTestSandbox" : "copyCheckstyleArtifactsToSandbox";
        final TaskProvider<Copy> taskProvider = tasks.register(taskName, Copy.class);
        taskProvider.configure((Copy copyTask) -> {
            copyTask.setGroup("intellij");
            copyTask.setDescription("Adds the gathered Checkstyle artifacts to the prepared "
                    + (test ? "test " : "") + "sandbox");

            final GatherCheckstyleArtifactsTask gatherTask =
                    (GatherCheckstyleArtifactsTask) tasks.getByName(GatherCheckstyleArtifactsTask.NAME);
            copyTask.dependsOn(gatherTask, "prepareTestingSandbox");
            if (test) {
                tasks.getByName(JavaPlugin.TEST_TASK_NAME).dependsOn(copyTask);
                tasks.getByName(CsaccessTestTask.NAME).dependsOn(copyTask);
                forEachXTest(tasks, xTask -> xTask.dependsOn(copyTask));
            } else {
                tasks.getByName("buildSearchableOptions").dependsOn(copyTask);
            }

            copyTask.from(gatherTask.getBundledJarsDir());
            copyTask.into(new File(project.getBuildDir(), "idea-sandbox/plugins" + (test ? "-test" : "")
                    + "/CheckStyle-IDEA/" + CSLIB_TARGET_SUBFOLDER));
        });
    }

    private void forEachXTest(final TaskContainer tasks, final Consumer<Task> taskConsumer) {
        supportedCsVersions.getVersions().forEach((final String csVersion) -> {
            if (!supportedCsVersions.getBaseVersion().equals(csVersion)) {
                taskConsumer.accept(tasks.getByName(CsaccessTestTask.getTaskName(csVersion)));
            }
        });
    }


    /**
     * This task makes the compiled classes and resources from the 'csaccess' sourceset available to the plugin by
     * copying it to the sandbox. Test code from csaccessTest sourceset not affected.
     *
     * @param pProject the Gradle project
     * @param pIsTest {@code true} if the target is the test sandbox, {@code false} for the main sandbox
     */
    private void createCopyClassesToSandboxTask(final Project pProject, final boolean pIsTest) {
        final TaskContainer tasks = pProject.getTasks();
        final String taskName = pIsTest ? "copyClassesToTestSandbox" : "copyClassesToSandbox";
        final TaskProvider<Copy> taskProvider = tasks.register(taskName, Copy.class);
        taskProvider.configure((Copy copyTask) -> {
            copyTask.setGroup("intellij");
            copyTask.setDescription("Copy classes from '" + CustomSourceSetCreator.CSACCESS_SOURCESET_NAME
                    + "' sourceset into the prepared " + (pIsTest ? "test " : "") + "sandbox");

            final JavaPluginExtension jpc = pProject.getExtensions().getByType(JavaPluginExtension.class);
            SourceSet csaccessSourceSet = jpc.getSourceSets().getByName(CustomSourceSetCreator.CSACCESS_SOURCESET_NAME);
            copyTask.dependsOn(tasks.getByName(csaccessSourceSet.getClassesTaskName()));
            if (pIsTest) {
                tasks.getByName(JavaPlugin.TEST_TASK_NAME).dependsOn(copyTask);
                tasks.getByName(CsaccessTestTask.NAME).dependsOn(copyTask);
                forEachXTest(tasks, xTask -> xTask.dependsOn(copyTask));
            } else {
                tasks.getByName("buildSearchableOptions").dependsOn(copyTask);
            }

            final String targetSubfolder = "checkstyle/classes";
            copyTask.from(csaccessSourceSet.getOutput());
            copyTask.into(new File(pProject.getBuildDir(), "idea-sandbox/plugins" + (pIsTest ? "-test" : "")
                    + "/CheckStyle-IDEA/" + targetSubfolder));
        });
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
