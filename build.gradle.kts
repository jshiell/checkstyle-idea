import org.infernus.idea.checkstyle.build.CheckstyleVersions
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask

plugins {
    id("java")
    id("jacoco")
    id("idea")
    id("org.jetbrains.intellij.platform") version "2.0.1"
    id("com.dorongold.task-tree") version "2.1.1"
    id("org.infernus.idea.checkstyle.build")
}

version = "5.94.1"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "CheckStyle-IDEA"
        name = "CheckStyle-IDEA"
        version = project.version.toString()

        ideaVersion {
            sinceBuild = "231.9392.1"
            untilBuild = provider { null }
        }
    }

    publishing {
        token.set(System.getenv("JETBRAINS_PLUGIN_REPO_TOKEN"))
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

}

tasks {
    // doesn't work, maybe waiting on https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1723
//    prepareSandbox {
//        sandboxDirectory = intellijPlatform.sandboxContainer.dir("current")
//    }

    withType<VerifyPluginTask> {
        dependsOn(copyClassesToSandbox, copyCheckstyleArtifactsToSandbox)
    }

    withType<Test> {
        setForkEvery(1)
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:deprecation"))
        options.release.set(17)

        if (name == "compileCsaccessJava" || name == "compileCsaccessTestJava") {
            options.compilerArgs.addAll(listOf("-Xlint:unchecked"))
        }
    }
}

// workaround for Checkstyle#14123
configurations.configureEach {
    resolutionStrategy.capabilitiesResolution.withCapability("com.google.collections:google-collections") {
        select("com.google.guava:guava:0")
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2023.1.5")

        bundledPlugin("com.intellij.java")

        instrumentationTools()
        testFramework(TestFrameworkType.Platform)
    }

    implementation("commons-io:commons-io:2.15.1")
    implementation("commons-codec:commons-codec:1.16.0")

    val checkStyleBaseVersion = (project.extra["supportedCsVersions"] as CheckstyleVersions).baseVersion
    csaccessCompileOnly("com.puppycrawl.tools:checkstyle:${checkStyleBaseVersion}") {
        exclude(group = "commons-logging", module = "commons-logging")
    }

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("org.mockito:mockito-core:5.5.0")
}

idea.module {
    isDownloadJavadoc = true
    isDownloadSources = true

    excludeDirs.addAll(listOf(file(".idea"), file("_support")))

    // TODO We should also tell IntelliJ automatically that csaccessTest contains test code.
    // The following lines should really do it, but currently don't, which seems like a Gradle bug to me:
    //val SourceSet catSourceSet = sourceSets.getByName(CustomSourceSetCreator.CSACCESSTEST_SOURCESET_NAME)
    //testSourceDirs.addAll(catSourceSet.getJava().getSrcDirs())
    //testSourceDirs.addAll(catSourceSet.getResources().getSrcDirs())
    //scopes.TEST.plus.addAll(listOf(configurations.getByName(catSourceSet.getRuntimeConfigurationName())))
}
