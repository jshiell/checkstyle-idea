package org.infernus.idea.checkstyle.build;

import java.io.File;
import java.math.BigDecimal;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension;
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension;
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification;
import org.gradle.testing.jacoco.tasks.JacocoReport;
import org.gradle.testing.jacoco.tasks.JacocoReportBase;
import org.gradle.testing.jacoco.tasks.rules.JacocoViolationRule;


public class CustomSourceSetCreator {
    static final String CSACCESS_SOURCESET_NAME = "csaccess";
    public static final String CSACCESSTEST_SOURCESET_NAME = "csaccessTest";

    private static final double MINIMUM_CSACCESS_COVERAGE = 0.70d;

    private final Project project;


    public CustomSourceSetCreator(final Project project) {
        this.project = project;
    }


    private String getJacocoReportTaskName() {
        final SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
        return sourceSets.getByName(CSACCESS_SOURCESET_NAME).getTaskName("jacoco", "report");
    }

    private String getJacocoVerificationTaskName() {
        final SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
        return sourceSets.getByName(CSACCESS_SOURCESET_NAME).getTaskName("jacoco", "CoverageVerification");
    }


    public void establishCsAccessSourceSet() {
        final SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
        final SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        // Create the 'csaccess' source set
        final SourceSet csaccessSourceSet = sourceSets.create(CSACCESS_SOURCESET_NAME);
        csaccessSourceSet.setCompileClasspath(csaccessSourceSet.getCompileClasspath().plus(mainSourceSet.getOutput()));
        csaccessSourceSet.setRuntimeClasspath(csaccessSourceSet.getRuntimeClasspath().plus(mainSourceSet.getOutput()));

        // Derive all its configurations from 'main', so 'csaccess' code can see 'main' code
        final ConfigurationContainer configurations = project.getConfigurations();
        configurations.getByName(csaccessSourceSet.getImplementationConfigurationName())
                .extendsFrom(configurations.getByName(mainSourceSet.getImplementationConfigurationName()));
        configurations.getByName(csaccessSourceSet.getCompileOnlyConfigurationName())
                .extendsFrom(configurations.getByName(mainSourceSet.getCompileOnlyConfigurationName()));
        configurations.getByName(csaccessSourceSet.getCompileClasspathConfigurationName())
                .extendsFrom(configurations.getByName(mainSourceSet.getCompileClasspathConfigurationName()));
        configurations.getByName(csaccessSourceSet.getRuntimeOnlyConfigurationName())
                .extendsFrom(configurations.getByName(mainSourceSet.getRuntimeOnlyConfigurationName()));

        // Wire task dependencies to match the classpath dependencies (arrow means "depends on"):
        //    - compileTestJava -> compileCsaccessJava
        //    - testClasses     -> csaccessClasses
        //    - jar             -> csaccessClasses
        final TaskContainer tasks = project.getTasks();
        tasks.getByName(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME)
                .dependsOn(tasks.getByName(csaccessSourceSet.getCompileJavaTaskName()));
        tasks.getByName(JavaPlugin.TEST_CLASSES_TASK_NAME)
                .dependsOn(tasks.getByName(csaccessSourceSet.getClassesTaskName()));
        tasks.getByName(JavaPlugin.JAR_TASK_NAME)
                .dependsOn(tasks.getByName(csaccessSourceSet.getClassesTaskName()));
    }


    public void establishCsAccessTestSourceSet() {
        final SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
        final SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        final SourceSet csaccessSourceSet = sourceSets.getByName(CSACCESS_SOURCESET_NAME);
        final SourceSet testSourceSet = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);

        // Create the 'csaccessTest' source set
        final SourceSet csaccessTestSourceSet = sourceSets.create(CSACCESSTEST_SOURCESET_NAME);
        csaccessTestSourceSet.setCompileClasspath(csaccessTestSourceSet.getCompileClasspath().
                plus(mainSourceSet.getOutput()).plus(csaccessSourceSet.getOutput()));
        csaccessTestSourceSet.setRuntimeClasspath(csaccessTestSourceSet.getRuntimeClasspath().
                plus(mainSourceSet.getOutput()).plus(csaccessSourceSet.getOutput()));

        // Derive all its configurations from 'test' and 'csaccess'
        final ConfigurationContainer configurations = project.getConfigurations();
        configurations.getByName(csaccessTestSourceSet.getImplementationConfigurationName()).extendsFrom(
                configurations.getByName(csaccessSourceSet.getImplementationConfigurationName()),
                configurations.getByName(testSourceSet.getImplementationConfigurationName()));
        configurations.getByName(csaccessTestSourceSet.getCompileOnlyConfigurationName()).extendsFrom(
                configurations.getByName(csaccessSourceSet.getCompileOnlyConfigurationName()),
                configurations.getByName(testSourceSet.getCompileOnlyConfigurationName()));
        configurations.getByName(csaccessTestSourceSet.getCompileClasspathConfigurationName()).extendsFrom(
                configurations.getByName(csaccessSourceSet.getCompileClasspathConfigurationName()),
                configurations.getByName(testSourceSet.getCompileClasspathConfigurationName()));
        configurations.getByName(csaccessTestSourceSet.getRuntimeOnlyConfigurationName()).extendsFrom(
                configurations.getByName(csaccessSourceSet.getRuntimeOnlyConfigurationName()),
                configurations.getByName(testSourceSet.getRuntimeOnlyConfigurationName()));

        // Wire task dependencies to match the classpath dependencies (arrow means "depends on"):
        //    - compileCsaccessTestJava -> compileCsaccessJava
        //    - csaccessTestClasses     -> csaccessClasses
        final TaskContainer tasks = project.getTasks();
        tasks.getByName(csaccessTestSourceSet.getCompileJavaTaskName())
                .dependsOn(tasks.getByName(csaccessSourceSet.getCompileJavaTaskName()));
        tasks.getByName(csaccessTestSourceSet.getClassesTaskName())
                .dependsOn(tasks.getByName(csaccessSourceSet.getClassesTaskName()));
    }


    public void setupCoverageVerification() {
        final TaskContainer tasks = project.getTasks();

        // Disable JaCoCo for 'test' source set
        final JacocoTaskExtension jacocoTestTaskExtension = (JacocoTaskExtension) tasks.getByName(
                JavaPlugin.TEST_TASK_NAME).getExtensions().getByName(JacocoPluginExtension.TASK_EXTENSION_NAME);
        jacocoTestTaskExtension.setEnabled(false);
        tasks.getByName("jacocoTestReport").setEnabled(false);
        tasks.getByName("jacocoTestCoverageVerification").setEnabled(false);

        // Enable JaCoCo reporting for 'csaccess' source set
        final String reportTaskName = getJacocoReportTaskName();
        TaskProvider<JacocoReport> provider = tasks.register(reportTaskName, JacocoReport.class);
        provider.configure((JacocoReport jacocoReportTask) -> {
            jacocoReportTask.dependsOn(tasks.getByName(CsaccessTestTask.NAME),
                    tasks.getByName(CsaccessTestTask.XTEST_TASK_NAME));
            jacocoReportTask.setDescription("Generate exclusive JaCoCo test report on the '"
                    + CSACCESS_SOURCESET_NAME + "' classes");
            configureJacocoTask(jacocoReportTask);
            jacocoReportTask.getReports().getXml().getRequired().set(true);
            jacocoReportTask.getReports().getCsv().getRequired().set(false);
            jacocoReportTask.getReports().getHtml().getRequired().set(true);
        });

        // Verify minimum line coverage for 'csaccess' source set
        final String verificationTaskName = getJacocoVerificationTaskName();
        TaskProvider<JacocoCoverageVerification> provider2 = tasks.register(verificationTaskName,
                JacocoCoverageVerification.class);
        provider2.configure((JacocoCoverageVerification jacocoVerificationTask) -> {
            jacocoVerificationTask.dependsOn(reportTaskName);
            jacocoVerificationTask.setDescription("Ensure that '" + CSACCESS_SOURCESET_NAME
                    + "' test coverage does not drop below a certain level");
            configureJacocoTask(jacocoVerificationTask);
            jacocoVerificationTask.getViolationRules().rule((final JacocoViolationRule rule) ->
                    rule.limit(jacocoLimit ->
                            jacocoLimit.setMinimum(BigDecimal.valueOf(MINIMUM_CSACCESS_COVERAGE))));
        });

        // Wire 'build' task so that it ensures coverage
        tasks.getByName(LifecycleBasePlugin.BUILD_TASK_NAME).dependsOn(verificationTaskName);
    }


    private void configureJacocoTask(final JacocoReportBase jacocoTask) {
        jacocoTask.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
        final SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
        final SourceSet csaccessSourceSet = sourceSets.getByName(CSACCESS_SOURCESET_NAME);
        jacocoTask.getClassDirectories().from(csaccessSourceSet.getOutput().getClassesDirs());
        jacocoTask.getSourceDirectories().from(csaccessSourceSet.getJava().getSourceDirectories());

        final FileCollection execFiles = project.files(project.getTasks().withType(CsaccessTestTask.class).stream()
                .map((final CsaccessTestTask task) -> new File(project.getBuildDir() + "/jacoco", task.getName() + ".exec"))
                .collect(Collectors.toList()));
        jacocoTask.getExecutionData().from(execFiles);
    }
}
