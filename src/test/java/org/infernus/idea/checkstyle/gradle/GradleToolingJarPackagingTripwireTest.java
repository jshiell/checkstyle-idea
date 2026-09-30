package org.infernus.idea.checkstyle.gradle;

import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The gradleTooling jar is injected into an arbitrary target project's Gradle daemon via IntelliJ's
 * generated initscript, so it must be a standalone jar carrying only our own tooling classes and its
 * ServiceLoader registration — never IntelliJ platform classes, which the injection mechanism
 * (GradleInitScriptUtil) explicitly refuses to inject alongside. Nothing else in {@code ./gradlew build}
 * or {@code verifyPlugin} would catch a missing services file or an accidentally-fat jar, so this is the
 * feature's only automated protection against a packaging regression.
 */
class GradleToolingJarPackagingTripwireTest {

    private static final String SERVICE_FILE =
            "META-INF/services/org.jetbrains.plugins.gradle.tooling.ModelBuilderService";
    private static final String EXPECTED_MODEL_BUILDER =
            "org.infernus.idea.checkstyle.gradle.tooling.CheckstyleGradleModelBuilder";

    @Test
    void jarExists() {
        assertTrue(gradleToolingJarFile().isFile(),
                "gradleToolingJar not found at " + gradleToolingJarFile());
    }

    @Test
    void jarRegistersTheModelBuilderViaServiceLoader() throws IOException {
        try (JarFile jarFile = new JarFile(gradleToolingJarFile())) {
            final JarEntry entry = jarFile.getJarEntry(SERVICE_FILE);
            assertTrue(entry != null, SERVICE_FILE + " missing from " + gradleToolingJarFile());

            try (InputStream stream = jarFile.getInputStream(entry)) {
                final String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim();
                assertThat(content, is(EXPECTED_MODEL_BUILDER));
            }
        }
    }

    @Test
    void jarContainsNoIntelliJPlatformClasses() throws IOException {
        try (JarFile jarFile = new JarFile(gradleToolingJarFile())) {
            final Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                final String name = entries.nextElement().getName();
                assertFalse(name.startsWith("com/intellij/"),
                        "gradleToolingJar must contain no IntelliJ platform classes, found: " + name);
            }
        }
    }

    @Test
    void jarClassesTargetNoNewerThanJava8() throws IOException {
        // class file major_version 52 == Java 8; the target project's own Gradle daemon may run an
        // older JDK than the one used to build this plugin, and cannot load classes above its own version.
        final int maxSupportedMajorVersion = 52;

        try (JarFile jarFile = new JarFile(gradleToolingJarFile())) {
            final Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }

                try (DataInputStream stream = new DataInputStream(jarFile.getInputStream(entry))) {
                    stream.skipBytes(6); // magic (4 bytes) + minor_version (2 bytes)
                    final int majorVersion = stream.readUnsignedShort();
                    assertTrue(majorVersion <= maxSupportedMajorVersion,
                            entry.getName() + " has class file major version " + majorVersion
                                    + ", which is newer than Java 8 (" + maxSupportedMajorVersion + ")");
                }
            }
        }
    }

    private static File gradleToolingJarFile() {
        final String path = System.getProperty("gradletooling.jar.path");
        if (path == null) {
            fail("System property gradletooling.jar.path not set - run via ./gradlew test");
        }
        return new File(path);
    }
}
