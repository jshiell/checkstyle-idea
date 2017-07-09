package org.infernus.idea.checkstyle.build;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension;
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension;
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification;
import org.gradle.testing.jacoco.tasks.JacocoReport;
import org.gradle.testing.jacoco.tasks.JacocoReportBase;
import org.gradle.testing.jacoco.tasks.rules.JacocoLimit;
import org.gradle.testing.jacoco.tasks.rules.JacocoViolationRule;

import java.io.File;
import java.math.BigDecimal;
import java.util.stream.Collectors;


public class CustomSourceSetCreator {
    public static final String CSACCESS_SOURCESET_NAME = "csaccess";
    public static final String CSACCESSTEST_SOURCESET_NAME = "csaccessTest";
    public static final String JACOCO_REPORT_TASK_NAME =
            "jacoco" + capitalize(CSACCESS_SOURCESET_NAME) + "Report";
    public static final String JACOCO_VERIFICATION_TASK_NAME =
            "jacoco" + capitalize(CSACCESS_SOURCESET_NAME) + "CoverageVerification";

    private static final double MINIMUM_CSACCESS_COVERAGE = 0.88d;

    private final Project project;


    public CustomSourceSetCreator(final Project pProject) {
        project = pProject;
    }


    private static String capitalize(final String pString) {
        String result = pString;
        if (pString != null) {
            final int strLen = pString.length();
            if (strLen > 0) {
                result = new StringBuilder(strLen + 1).append(Character.toTitleCase(pString.charAt(0)))
                        .append(pString.substring(1)).toString();
            }
        }
        return result;
    }


    public CustomSourceSetCreator establishCsAccessSourceSet() {

        final SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
        final SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        // Create the 'csaccess' source set
        final SourceSet csaccessSourceSet = sourceSets.create(CSACCESS_SOURCESET_NAME);
        csaccessSourceSet.setCompileClasspath(csaccessSourceSet.getCompileClasspath().plus(mainSourceSet.getOutput()));
        csaccessSourceSet.setRuntimeClasspath(csaccessSourceSet.getRuntimeClasspath().plus(mainSourceSet.getOutput()));
        sourceSets.add(csaccessSourceSet);

        // Derive all its configurations from 'main', so 'csaccess' code can see 'main' code
        final ConfigurationContainer configurations = project.getConfigurations();
        final Configuration compileConfig = configurations.getByName(mainSourceSet.getCompileConfigurationName());
        final Configuration compileOnlyConfig = configurations.getByName(mainSourceSet
                .getCompileOnlyConfigurationName());
        final Configuration compileClasspathConfig = configurations.getByName(mainSourceSet
                .getCompileClasspathConfigurationName());
        final Configuration runtimeConfig = configurations.getByName(mainSourceSet.getRuntimeConfigurationName());
        configurations.getByName(csaccessSourceSet.getCompileConfigurationName()).extendsFrom(compileConfig);
        configurations.getByName(csaccessSourceSet.getCompileOnlyConfigurationName()).extendsFrom(compileOnlyConfig);
        configurations.getByName(csaccessSourceSet.getCompileClasspathConfigurationName()).extendsFrom(compileClasspathConfig);
        configurations.getByName(csaccessSourceSet.getRuntimeConfigurationName()).extendsFrom(runtimeConfig);

        // Wire task dependencies to match the classpath dependencies (arrow means "depends on"):
        //    - compileTestJava -> compileCsaccessJava
        //    - testClasses     -> csaccessClasses
        //    - jar             -> csaccessClasses
        final TaskContainer tasks = project.getTasks();
        tasks.getByName(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME).dependsOn(tasks.getByName(csaccessSourceSet
                .getCompileJavaTaskName()));
        tasks.getByName(JavaPlugin.TEST_CLASSES_TASK_NAME).dependsOn(tasks.getByName(csaccessSourceSet
                .getClassesTaskName()));
        tasks.getByName(JavaPlugin.JAR_TASK_NAME).dependsOn(tasks.getByName(csaccessSourceSet.getClassesTaskName()));

        return this;
    }


    public CustomSourceSetCreator establishCsAccessTestSourceSet() {

        final SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
        final SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        final SourceSet csaccessSourceSet = sourceSets.getByName(CSACCESS_SOURCESET_NAME);
        final SourceSet testSourceSet = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);

        // Create the 'csaccess' source set
        final SourceSet csaccessTestSourceSet = sourceSets.create(CSACCESSTEST_SOURCESET_NAME);
        csaccessTestSourceSet.setCompileClasspath(csaccessTestSourceSet.getCompileClasspath().
                plus(mainSourceSet.getOutput()).plus(csaccessSourceSet.getOutput()));
        csaccessTestSourceSet.setRuntimeClasspath(csaccessTestSourceSet.getRuntimeClasspath().
                plus(mainSourceSet.getOutput()).plus(csaccessSourceSet.getOutput()));
        sourceSets.add(csaccessTestSourceSet);

        // Derive all its configurations from 'test' and 'csaccess'
        final ConfigurationContainer configurations = project.getConfigurations();
        final Configuration csaccessCompileConfig = configurations.getByName(
                csaccessSourceSet.getCompileConfigurationName());
        final Configuration csaccessCompileOnlyConfig = configurations.getByName(
                csaccessSourceSet.getCompileOnlyConfigurationName());
        final Configuration csaccessCompileClasspathConfig = configurations.getByName(
                csaccessSourceSet.getCompileClasspathConfigurationName());
        final Configuration csaccessRuntimeConfig = configurations.getByName(
                csaccessSourceSet.getRuntimeConfigurationName());
        final Configuration testCompileConfig = configurations.getByName(testSourceSet.getCompileConfigurationName());
        final Configuration testCompileOnlyConfig = configurations.getByName(
                testSourceSet.getCompileOnlyConfigurationName());
        final Configuration testCompileClasspathConfig = configurations.getByName(
                testSourceSet.getCompileClasspathConfigurationName());
        final Configuration testRuntimeConfig = configurations.getByName(testSourceSet.getRuntimeConfigurationName());
        configurations.getByName(csaccessTestSourceSet.getCompileConfigurationName()).
                extendsFrom(csaccessCompileConfig, testCompileConfig);
        configurations.getByName(csaccessTestSourceSet.getCompileOnlyConfigurationName()).
                extendsFrom(csaccessCompileOnlyConfig, testCompileOnlyConfig);
        configurations.getByName(csaccessTestSourceSet.getCompileClasspathConfigurationName()).
                extendsFrom(csaccessCompileClasspathConfig, testCompileClasspathConfig);
        configurations.getByName(csaccessTestSourceSet.getRuntimeConfigurationName()).
                extendsFrom(csaccessRuntimeConfig, testRuntimeConfig);

        // Wire task dependencies to match the classpath dependencies (arrow means "depends on"):
        //    - compileCsaccessTestJava -> compileCsaccessJava
        //    - csaccessTestClasses     -> csaccessClasses
        final TaskContainer tasks = project.getTasks();
        tasks.getByName(csaccessTestSourceSet.getCompileJavaTaskName()).dependsOn(tasks.getByName(csaccessSourceSet
                .getCompileJavaTaskName()));
        tasks.getByName(csaccessTestSourceSet.getClassesTaskName()).dependsOn(tasks.getByName(csaccessSourceSet
                .getClassesTaskName()));

        return this;
    }


    public void setupCoverageVerification() {
        final TaskContainer tasks = project.getTasks();

        // Disable JaCoCo for 'test' source set
        final JacocoTaskExtension jacocoTestTaskExtension = (JacocoTaskExtension) tasks.getByName(
                JavaPlugin.TEST_TASK_NAME).getExtensions().getByName(JacocoPluginExtension.TASK_EXTENSION_NAME);
        jacocoTestTaskExtension.setEnabled(false);
        tasks.remove(tasks.getByName("jacocoTestReport"));
        tasks.remove(tasks.getByName("jacocoTestCoverageVerification"));

        // Enable JaCoCo reporting for 'csaccess' source set
        final JacocoReport jacocoReportTask = tasks.create(JACOCO_REPORT_TASK_NAME, JacocoReport.class);
        jacocoReportTask.dependsOn(tasks.getByName(CsaccessTestTask.NAME),
                tasks.getByName(CsaccessTestTask.XTEST_TASK_NAME));
        jacocoReportTask.setDescription("Generate exclusive JaCoCo test report on the '"
                + CSACCESS_SOURCESET_NAME + "' classes");
        configureJacocoTask(jacocoReportTask);
        jacocoReportTask.getReports().getXml().setEnabled(true);
        jacocoReportTask.getReports().getCsv().setEnabled(false);
        jacocoReportTask.getReports().getHtml().setEnabled(true);

        // Verify minimum line coverage for 'csaccess' source set
        final JacocoCoverageVerification jacocoVerificationTask = tasks.create(JACOCO_VERIFICATION_TASK_NAME,
                JacocoCoverageVerification.class);
        jacocoVerificationTask.dependsOn(jacocoReportTask);
        jacocoVerificationTask.setDescription("Ensure that '" + CSACCESS_SOURCESET_NAME
                + "' test coverage does not drop below a certain level");
        configureJacocoTask(jacocoVerificationTask);
        jacocoVerificationTask.getViolationRules().rule((final JacocoViolationRule rule) -> {
            rule.limit((final JacocoLimit jacocoLimit) -> {
                jacocoLimit.setMinimum(BigDecimal.valueOf(MINIMUM_CSACCESS_COVERAGE));
            });
        });

        // Wire 'build' task so that it ensures coverage
        tasks.getByName(LifecycleBasePlugin.BUILD_TASK_NAME).dependsOn(jacocoVerificationTask);
    }


    private void configureJacocoTask(final JacocoReportBase pJacocoTask) {
        pJacocoTask.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
        final SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get("sourceSets");
        final SourceSet csaccessSourceSet = sourceSets.getByName(CSACCESS_SOURCESET_NAME);
        pJacocoTask.setClassDirectories(project.files(csaccessSourceSet.getOutput().getClassesDir()));
        pJacocoTask.setSourceDirectories(csaccessSourceSet.getJava().getSourceDirectories());

        final FileCollection execFiles = project.files(project.getTasks().withType(CsaccessTestTask.class).stream()
                .map((final CsaccessTestTask task) -> { //
                    return new File(project.getBuildDir() + "/jacoco", task.getName() + ".exec");
                }).collect(Collectors.toList()));
        pJacocoTask.setExecutionData(execFiles);
    }
}
