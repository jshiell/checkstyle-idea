import org.infernus.idea.checkstyle.build.CheckstyleVersions
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

plugins {
    id("java")
    id("jacoco")
    id("idea")
    id("org.jetbrains.intellij.platform") version "2.11.0"

    id("org.infernus.idea.checkstyle.build")
}

version = "26.2.0"

intellijPlatform {
    pluginConfiguration {
        id = "CheckStyle-IDEA"
        name = "CheckStyle-IDEA"
        version = project.version.toString()

        ideaVersion {
            untilBuild = provider { null }
        }
    }

    publishing {
        token.set(System.getenv("JETBRAINS_PLUGIN_REPO_TOKEN"))
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }

    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
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
        jvmArgs("-Xshare:off")
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:deprecation"))
        options.release.set(21)

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
        // Update reference in GradlePluginMain on change
        intellijIdeaCommunity("2024.3.7")

        bundledPlugin("com.intellij.java")

        testFramework(TestFrameworkType.Platform)
    }

    implementation("commons-io:commons-io:2.20.0")
    implementation("commons-codec:commons-codec:1.19.0")

    val checkStyleBaseVersion = (project.extra["supportedCsVersions"] as CheckstyleVersions).baseVersion
    csaccessCompileOnly("com.puppycrawl.tools:checkstyle:${checkStyleBaseVersion}") {
        exclude("commons-logging:commons-logging")
    }

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.hamcrest:hamcrest:3.0")
    testImplementation("org.mockito:mockito-core:5.18.0")
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
