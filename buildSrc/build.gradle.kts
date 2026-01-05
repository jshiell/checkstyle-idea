plugins {
    id("java-gradle-plugin")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("commons-io:commons-io:2.20.0")
    testImplementation("junit:junit:4.12")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation"))
}

gradlePlugin {
    plugins {
        create("simplePlugin") {
            id = "org.infernus.idea.checkstyle.build"
            implementationClass = "org.infernus.idea.checkstyle.build.GradlePluginMain"
        }
    }
}
