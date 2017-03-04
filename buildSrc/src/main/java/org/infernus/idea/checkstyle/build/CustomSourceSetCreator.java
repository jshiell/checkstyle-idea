package org.infernus.idea.checkstyle.build;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;


public class CustomSourceSetCreator {

    public static final String CSACCESS_SOURCESET_NAME = "csaccess";
    public static final String CSACCESSTEST_SOURCESET_NAME = "csaccessTest";

    private final Project project;

    public CustomSourceSetCreator(final Project pProject) {
        project = pProject;
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
        final Configuration compileConfig = configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME);
        final Configuration compileOnlyConfig = configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME);
        final Configuration compileClasspathConfig = configurations.getByName(JavaPlugin
                .COMPILE_CLASSPATH_CONFIGURATION_NAME);
        final Configuration runtimeConfig = configurations.getByName(JavaPlugin.RUNTIME_CONFIGURATION_NAME);
        configurations.getByName(csaccessSourceSet.getCompileConfigurationName()).extendsFrom(compileConfig);
        configurations.getByName(csaccessSourceSet.getCompileOnlyConfigurationName()).extendsFrom(compileOnlyConfig);
        configurations.getByName(csaccessSourceSet.getCompileClasspathConfigurationName())
                .extendsFrom(compileClasspathConfig);
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

        // Create the 'csaccess' source set
        final SourceSet csaccessTestSourceSet = sourceSets.create(CSACCESSTEST_SOURCESET_NAME);
        csaccessTestSourceSet.setCompileClasspath(csaccessTestSourceSet.getCompileClasspath()
                .plus(mainSourceSet.getOutput())
                .plus(csaccessSourceSet.getOutput()));
        csaccessTestSourceSet.setRuntimeClasspath(csaccessTestSourceSet.getRuntimeClasspath()
                .plus(mainSourceSet.getOutput())
                .plus(csaccessSourceSet.getOutput()));
        sourceSets.add(csaccessTestSourceSet);

        // Derive all its configurations from 'test' and 'csaccess'
        final ConfigurationContainer configurations = project.getConfigurations();
        final Configuration csaccessCompileConfig = configurations
                .getByName(csaccessSourceSet.getCompileConfigurationName());
        final Configuration csaccessCompileOnlyConfig = configurations.getByName(
                csaccessSourceSet.getCompileOnlyConfigurationName());
        final Configuration csaccessCompileClasspathConfig = configurations.getByName(
                csaccessSourceSet.getCompileClasspathConfigurationName());
        final Configuration csaccessRuntimeConfig = configurations.getByName(
                csaccessSourceSet.getRuntimeConfigurationName());
        final Configuration testCompileConfig = configurations.getByName(JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME);
        final Configuration testCompileOnlyConfig = configurations.getByName(
                JavaPlugin.TEST_COMPILE_ONLY_CONFIGURATION_NAME);
        final Configuration testCompileClasspathConfig = configurations.getByName(
                JavaPlugin.TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME);
        final Configuration testRuntimeConfig = configurations.getByName(JavaPlugin.TEST_RUNTIME_CONFIGURATION_NAME);
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
}
