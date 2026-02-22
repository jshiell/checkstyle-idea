package org.infernus.idea.checkstyle.checker;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.intellij.openapi.project.Project;
import org.hamcrest.core.IsEqual;
import org.infernus.idea.checkstyle.util.TempDirProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;


/**
 * Unit tests of {@link ClasspathStabilizer}.
 */
public class ClasspathStabilizerTest {
    private static final Project PROJECT = Mockito.mock(Project.class);

    @TempDir
    Path targetFolder;


    @BeforeAll
    public static void setup() {
        String baseDir = Objects.requireNonNull(ClasspathStabilizerTest.class.getResource("/cpstab")).getPath();
        Mockito.when(PROJECT.getBasePath()).thenReturn(baseDir);
    }


    private List<URL> buildInputClasspath() throws MalformedURLException {
        final String baseDir = PROJECT.getBasePath();
        List<URL> result = new ArrayList<>();
        result.add(new java.io.File(baseDir, "lib1.jar").toURI().toURL());
        result.add(new java.io.File(baseDir, "nonexistent.jar").toURI().toURL());
        result.add(new java.io.File(baseDir, "libs/lib1.jar").toURI().toURL());
        result.add(new java.io.File(baseDir, "libs/lib2.jar").toURI().toURL());
        result.add(new java.io.File(baseDir, "hashed/lib1.jar").toURI().toURL());
        result.add(new java.io.File(baseDir, "a_long_folder/path_more_than_50_characters/in_total/lib1.jar").toURI().toURL());
        // SHA1("hashed") == "mmmfkoez3kxcsqlrrqbfbcxo5e4k6gq4"
        result.add(new java.io.File(baseDir, "mmmfkoez3kxcsqlrrqbfbcxo5e4k6gq4/lib1.jar").toURI().toURL());
        result.add(new java.io.File(baseDir, TempDirProvider.README_FILE).toURI().toURL());
        return result;
    }


    @Test
    public void testStabilizer() throws IOException {
        final List<URL> inputClasspath = buildInputClasspath();
        final ClasspathStabilizer underTest = new ClasspathStabilizer(PROJECT, targetFolder);

        final URL[] result = underTest.stabilize(inputClasspath);

        final String baseDir = PROJECT.getBasePath();
        final URL[] expected = new URL[]{
                targetFolder.resolve("lib1.jar").toUri().toURL(),
                new java.io.File(baseDir, "nonexistent.jar").toURI().toURL(),
                targetFolder.resolve("libs/lib1.jar").toUri().toURL(),
                targetFolder.resolve("libs/lib2.jar").toUri().toURL(),
                targetFolder.resolve(ClasspathStabilizer.HASHFOLDER
                        + "/mmmfkoez3kxcsqlrrqbfbcxo5e4k6gq4/lib1.jar").toUri().toURL(),
                targetFolder.resolve(ClasspathStabilizer.HASHFOLDER
                        + "/n6yrt7uzrjravbwjwxa2ed62cpl57vh5/lib1.jar").toUri().toURL(),
                targetFolder.resolve("mmmfkoez3kxcsqlrrqbfbcxo5e4k6gq4/lib1.jar").toUri().toURL(),
                targetFolder.resolve(ClasspathStabilizer.HASHFOLDER
                        + "/3i42h3s6nnfq2msvx7xzkyayscx5qbyj/" + TempDirProvider.README_FILE).toUri().toURL()
        };
        assertThat(expected, IsEqual.equalTo(result));
    }


    @Test
    public void testUpdate() throws IOException, InterruptedException {
        final String baseDir = PROJECT.getBasePath();
        final List<URL> inputClasspath = new ArrayList<>();
        inputClasspath.add(new java.io.File(baseDir, "lib1.jar").toURI().toURL());
        final ClasspathStabilizer underTest = new ClasspathStabilizer(PROJECT, targetFolder);

        URL[] result = underTest.stabilize(inputClasspath);
        final URL[] expected = new URL[]{targetFolder.resolve("lib1.jar").toUri().toURL()};
        assertArrayEquals(expected, result);

        Thread.sleep(TimeUnit.SECONDS.toMillis(2));

        result = underTest.stabilize(inputClasspath);
        assertArrayEquals(expected, result);
    }
}
