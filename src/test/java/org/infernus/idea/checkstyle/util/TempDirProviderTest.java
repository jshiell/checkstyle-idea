package org.infernus.idea.checkstyle.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;


/**
 * Unit tests of {@link TempDirProvider}.
 */
public class TempDirProviderTest {
    private static final Project PROJECT = Mockito.mock(Project.class);

    @Rule
    public final TemporaryFolder targetFolder = new TemporaryFolder();


    private static class TempDirProvider4Test
            extends TempDirProvider {
        private final boolean usesIdeaFolder;

        private final TemporaryFolder junitTempFolder;

        TempDirProvider4Test(final boolean pUsesIdeaFolder, @NotNull final TemporaryFolder pJunitTempFolder) {
            usesIdeaFolder = pUsesIdeaFolder;
            junitTempFolder = pJunitTempFolder;
        }

        @Override
        Optional<VirtualFile> getIdeaFolder(@NotNull final Project project) {
            if (usesIdeaFolder) {
                final VirtualFile vf = Mockito.mock(VirtualFile.class);
                Mockito.when(vf.getPath()).thenReturn(junitTempFolder.getRoot().getAbsolutePath());
                return Optional.of(vf);
            }
            return Optional.empty();
        }
    }


    @BeforeClass
    public static void setup() {
        Mockito.when(PROJECT.getName()).thenReturn("project-TempDirProviderTest");
        Mockito.when(PROJECT.getLocationHash()).thenReturn("f2d57494");
    }


    @Test
    public void testCopyLibsTargetIdea() {
        TempDirProvider underTest = new TempDirProvider4Test(true, targetFolder);
        Optional<File> result = underTest.forCopiedLibraries(PROJECT);
        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(Files.isRegularFile(Paths.get(targetFolder.getRoot().toURI())
                .resolve("checkstyleidea-libs").resolve(TempDirProvider.README_FILE)));
    }


    @Test
    public void testCopyLibsTargetTemp() {
        Optional<File> result = Optional.empty();
        try {
            TempDirProvider underTest = new TempDirProvider4Test(false, targetFolder);
            result = underTest.forCopiedLibraries(PROJECT);
            Assert.assertTrue(result.isPresent());
            Assert.assertTrue(new File(System.getProperty("java.io.tmpdir"), "csi-f2d57494-libs/" + TempDirProvider
                    .README_FILE).isFile());
        } finally {
            deleteTempDir(result);
        }
    }


    @Test
    public void testCopyLibsTargetTempTwice() {
        Optional<File> result1 = Optional.empty();
        Optional<File> result2 = Optional.empty();
        try {
            TempDirProvider underTest = new TempDirProvider4Test(false, targetFolder);
            result1 = underTest.forCopiedLibraries(PROJECT);
            result2 = underTest.forCopiedLibraries(PROJECT);
            Assert.assertTrue(result1.isPresent());
            Assert.assertTrue(result2.isPresent());
            Assert.assertEquals(result1.get(), result2.get());
        } finally {
            deleteTempDir(result1);
            deleteTempDir(result2);
        }
    }


    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void deleteTempDir(@NotNull final Optional<File> pTempDir) {
        pTempDir.ifPresent(FileUtils::deleteQuietly);
    }
}
