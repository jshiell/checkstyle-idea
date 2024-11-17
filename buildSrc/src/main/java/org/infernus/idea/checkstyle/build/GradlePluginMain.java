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
import org.jetbrains.annotations.NotNull;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.println;

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
        wireIntellijPluginTasks(project);
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
            SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            mainSourceSet.getResources().srcDir(task.getClassPathsInfoFile().getParentFile());
        });

        createCopyCheckstyleArtifactsToSandboxTask(project, false);
        createCopyCheckstyleArtifactsToSandboxTask(project, true);

        createCopyClassesToSandboxTask(project, false);
        createCopyClassesToSandboxTask(project, true);
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
            copyTask.dependsOn(gatherTask, "prepareTestSandbox");
            if (test) {
                tasks.getByName(JavaPlugin.TEST_TASK_NAME).dependsOn(copyTask);
                tasks.getByName(CsaccessTestTask.NAME).dependsOn(copyTask);
                forEachXTest(tasks, xTask -> xTask.dependsOn(copyTask));
            } else {
                tasks.getByName("buildSearchableOptions").dependsOn(copyTask);
            }

            copyTask.from(gatherTask.getBundledJarsDir());
            copyTask.into(new File(project.getLayout().getBuildDirectory().getAsFile().get(), pluginSandboxDir(test, CSLIB_TARGET_SUBFOLDER)));
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
     * @param project the Gradle project
     * @param test {@code true} if the target is the test sandbox, {@code false} for the main sandbox
     */
    private void createCopyClassesToSandboxTask(final Project project, final boolean test) {
        final TaskContainer tasks = project.getTasks();
        final String taskName = test ? "copyClassesToTestSandbox" : "copyClassesToSandbox";
        final TaskProvider<Copy> taskProvider = tasks.register(taskName, Copy.class);
        taskProvider.configure((Copy copyTask) -> {
            copyTask.setGroup("intellij");
            copyTask.setDescription("Copy classes from '" + CustomSourceSetCreator.CSACCESS_SOURCESET_NAME
                    + "' sourceset into the prepared " + (test ? "test " : "") + "sandbox");

            final JavaPluginExtension jpc = project.getExtensions().getByType(JavaPluginExtension.class);
            SourceSet csaccessSourceSet = jpc.getSourceSets().getByName(CustomSourceSetCreator.CSACCESS_SOURCESET_NAME);
            copyTask.dependsOn(tasks.getByName(csaccessSourceSet.getClassesTaskName()));
            if (test) {
                tasks.getByName(JavaPlugin.TEST_TASK_NAME).dependsOn(copyTask);
                tasks.getByName(CsaccessTestTask.NAME).dependsOn(copyTask);
                forEachXTest(tasks, xTask -> xTask.dependsOn(copyTask));
                copyTask.mustRunAfter(tasks.getByName("copyCheckstyleArtifactsToTestSandbox"));
            } else {
                tasks.getByName("buildSearchableOptions").dependsOn(copyTask);
                copyTask.mustRunAfter(tasks.getByName("copyCheckstyleArtifactsToSandbox"));
            }

            copyTask.from(csaccessSourceSet.getOutput());
            copyTask.into(new File(project.getLayout().getBuildDirectory().getAsFile().get(), pluginSandboxDir(test, CSCLASSES_TARGET_SUBFOLDER)));
        });
    }

    private @NotNull String pluginSandboxDir(final boolean test, final String subDirectory) {
        // must remain in sync with task configuration in Gradle file
        return "idea-sandbox/IC-2023.1.5/plugins"
                + (test ? "-test" : "")
                + "/checkstyle-idea/" + subDirectory;
    }

    /**
     * Defer some of the wiring until after the intellij plugin's tasks have been created.
     *
     * @param project the Gradle project
     */
    private void wireIntellijPluginTasks(final Project project) {
        final TaskContainer tasks = project.getTasks();
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
