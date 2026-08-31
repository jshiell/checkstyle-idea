import org.infernus.idea.checkstyle.build.CheckstyleVersions
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

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
    alias(libs.plugins.intellij.platform)

    id("org.infernus.idea.checkstyle.build")
}

version = "26.17.0"

intellijPlatform {
    pluginConfiguration {
        id = "CheckStyle-IDEA"
        name = "CheckStyle-IDEA"
        version = project.version.toString()

        ideaVersion {
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        freeArgs = listOf("-mute", "TemplateWordInPluginName")
    }

    publishing {
        token.set(System.getenv("JETBRAINS_PLUGIN_REPO_TOKEN"))
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

val mockitoAgent: Configuration = configurations.create("mockitoAgent") { isCanBeConsumed = false }

abstract class MockitoAgentProvider : CommandLineArgumentProvider {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val agentFiles: ConfigurableFileCollection

    override fun asArguments() = listOf("-javaagent:${agentFiles.asPath}")
}

abstract class GradleToolingJarPathProvider : CommandLineArgumentProvider {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val jarFile: RegularFileProperty

    override fun asArguments() = listOf("-Dgradletooling.jar.path=${jarFile.get().asFile.absolutePath}")
}

tasks {
    named<Test>("test") {
        // MavenMultiVersionImportingTestCase.getMavenVersions() is final as of IDEA 2025.1, so
        // MavenCheckstyleConfiguratorTest can no longer pin this by overriding it. Without the pin
        // that class runs against every bundled Maven version rather than just the default one.
        systemProperty("maven.versions.to.run", "bundled")
    }

    withType<Test> {
        jvmArgs("-Xshare:off")
        val agentProvider = objects.newInstance(MockitoAgentProvider::class)
        agentProvider.agentFiles.from(mockitoAgent)
        jvmArgumentProviders.add(agentProvider)
        useJUnitPlatform()

        // tests bind local HttpServer instances to OS-assigned ephemeral ports; without this, the
        // JDK's HttpURLConnection keep-alive cache (keyed on host:port only) can hand a request to a
        // pooled connection left over from an unrelated test whose server happened to reuse the same
        // port number, delivering that test's response instead of the current server's.
        systemProperty("http.keepAlive", "false")
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

// The 'gradleTooling' source set builds the ModelBuilderService that runs inside a *target project's*
// Gradle daemon (injected via IntelliJ's generated initscript), not inside the IDE process. It must
// compile against Gradle's own API only — no IntelliJ platform classes — so its output jar is safe to
// inject into an arbitrary Gradle daemon. Filtering these two jars out of 'main's resolved compile
// classpath (rather than hardcoding their cache-transform paths) means this stays correct across
// machines and IntelliJ Platform Gradle Plugin cache-path changes: 'main' already depends on
// bundledPlugin("com.intellij.gradle"), whose lib/ directory happens to contain exactly the Gradle API
// jars IntelliJ itself bundles for this purpose.
val gradleTooling: SourceSet = sourceSets.create("gradleTooling")
val gradleToolingTest: SourceSet = sourceSets.create("gradleToolingTest")

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(libs.versions.intellij.idea.community.get())

        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.idea.maven")
        bundledPlugin("com.intellij.gradle")
        bundledModule("intellij.platform.vcs.impl")

        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Maven)
    }

    // 'intellijPlatformBundledPlugins' is a leaf configuration (no extendsFrom back to 'compileOnly' /
    // 'implementation'), so resolving it here to source these two jars cannot form a dependency cycle
    // with 'gradleTooling's own compileOnly classpath — unlike referencing 'main's full compileClasspath,
    // which does extend back through 'compileOnly' once that also carries gradleTooling.output (below).
    add(gradleTooling.compileOnlyConfigurationName, files(provider {
        configurations.getByName("intellijPlatformBundledPlugins").filter { file ->
            file.name.startsWith("gradle-api-") || file.name == "gradle-tooling-extension-api.jar"
        }
    }))

    // 'gradleTooling's classes must be visible on 'main's classpath so GradleCheckstyleResolver can
    // reference CheckstyleGradleModel by static type; at runtime both land in the same plugin
    // classloader because gradleToolingJar is copied alongside the main jar into checkstyle-idea/lib/.
    // 'compileOnly' is not transitive to 'test' by default, so GradleCheckstyleResolverTest needs its
    // own testCompileOnly dependency to reference the same classes.
    compileOnly(gradleTooling.output)
    testCompileOnly(gradleTooling.output)

    add(gradleToolingTest.implementationConfigurationName, gradleTooling.output)
    add(gradleToolingTest.compileOnlyConfigurationName, files(provider {
        configurations.getByName("intellijPlatformBundledPlugins").filter { file ->
            file.name.startsWith("gradle-api-") || file.name == "gradle-tooling-extension-api.jar"
        }
    }))
    // ModelBuilderService (IntelliJ's own API, not part of Gradle's distribution) must also be present
    // at test runtime since the tests instantiate CheckstyleGradleModelBuilder directly. gradle-api
    // itself is deliberately NOT added here: at runtime CheckstyleExtension etc. come from
    // gradleTestKit()'s bundled Gradle version instead, and adding both would risk two competing
    // definitions of the same Gradle API classes on one classpath.
    add(gradleToolingTest.runtimeOnlyConfigurationName, files(provider {
        configurations.getByName("intellijPlatformBundledPlugins").filter { file ->
            file.name == "gradle-tooling-extension-api.jar"
        }
    }))
    add(gradleToolingTest.implementationConfigurationName, gradleTestKit())
    add(gradleToolingTest.implementationConfigurationName, libs.junit.jupiter.api)
    add(gradleToolingTest.runtimeOnlyConfigurationName, libs.junit.jupiter.engine)
    add(gradleToolingTest.runtimeOnlyConfigurationName, libs.junit.platform.launcher)
    add(gradleToolingTest.implementationConfigurationName, libs.hamcrest)
    add(gradleToolingTest.implementationConfigurationName, libs.mockito.core)
    add(gradleToolingTest.implementationConfigurationName, libs.mockito.junit.jupiter)

    implementation(libs.commons.io)
    implementation(libs.commons.codec)
    implementation(libs.maven.settings)
    implementation(libs.plexus.cipher)
    implementation(libs.plexus.sec.dispatcher)

    val checkStyleBaseVersion = (project.extra["supportedCsVersions"] as CheckstyleVersions).baseVersion
    csaccessCompileOnly("com.puppycrawl.tools:checkstyle:${checkStyleBaseVersion}") {
        exclude("commons-logging:commons-logging")
    }

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.vintage.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.hamcrest)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    mockitoAgent(libs.mockito.core) { isTransitive = false }
}

idea.module {
    isDownloadJavadoc = true
    isDownloadSources = true

    excludeDirs.addAll(listOf(
        file(".idea"),
        file("_support"),
        file(path = ".claude"),
        file(path = ".opencode"),
        file(path = ".settings"),
        file(path = ".local"),
        file(path = ".cache")))

    // TODO We should also tell IntelliJ automatically that csaccessTest contains test code.
    // The following lines should really do it, but currently don't, which seems like a Gradle bug to me:
    //val SourceSet catSourceSet = sourceSets.getByName(CustomSourceSetCreator.CSACCESSTEST_SOURCESET_NAME)
    //testSourceDirs.addAll(catSourceSet.getJava().getSrcDirs())
    //testSourceDirs.addAll(catSourceSet.getResources().getSrcDirs())
    //scopes.TEST.plus.addAll(listOf(configurations.getByName(catSourceSet.getRuntimeConfigurationName())))
}

val gradleToolingJar = tasks.register<Jar>("gradleToolingJar") {
    archiveBaseName.set("checkstyle-idea-gradle-tooling")
    from(gradleTooling.output)
    // The IntelliJ Platform Gradle Plugin patches every registered Jar task in this project to also
    // embed the main plugin descriptor; exclude it so this jar — which gets injected into an arbitrary
    // target project's Gradle daemon — carries nothing beyond our own tooling classes and services file.
    exclude("META-INF/plugin.xml")
}

listOf("prepareSandbox", "prepareTestSandbox").forEach { taskName ->
    tasks.named<Sync>(taskName) {
        dependsOn(gradleToolingJar)
        from(gradleToolingJar) { into("checkstyle-idea/lib") }
    }
}

tasks.named("jar") {
    dependsOn(gradleToolingJar)
}

// GradleToolingJarPackagingTripwireTest asserts the built jar's contents directly, so 'test' needs it
// built first and needs to know where it landed.
tasks.named<Test>("test") {
    dependsOn(gradleToolingJar)
    val jarPathProvider = objects.newInstance(GradleToolingJarPathProvider::class)
    jarPathProvider.jarFile.set(gradleToolingJar.flatMap { it.archiveFile })
    jvmArgumentProviders.add(jarPathProvider)
}

val gradleToolingTestTask = tasks.register<Test>("gradleToolingTest") {
    group = "verification"
    description = "Runs the plain-JUnit tests for the 'gradleTooling' source set."
    testClassesDirs = gradleToolingTest.output.classesDirs
    classpath = gradleToolingTest.runtimeClasspath
    useJUnitPlatform()
    // org.gradle.testfixtures.ProjectBuilder injects synthetic classes via a privateLookupIn handle on
    // java.lang, which the module system blocks by default outside Gradle's own daemon JVM.
    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}

tasks.named("check") {
    dependsOn(gradleToolingTestTask)
}
