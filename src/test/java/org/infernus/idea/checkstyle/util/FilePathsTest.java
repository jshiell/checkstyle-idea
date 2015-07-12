package org.infernus.idea.checkstyle.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FilePathsTest {

    @Test
    public void testGetRelativePathsUnix() {
        assertEquals("stuff/xyz.dat", FilePaths.relativePath("/var/data/stuff/xyz.dat", "/var/data/", "/"));
        assertEquals("../../b/c", FilePaths.relativePath("/a/b/c", "/a/x/y/", "/"));
        assertEquals("../../b/c", FilePaths.relativePath("/m/n/o/a/b/c", "/m/n/o/a/x/y/", "/"));
    }

    @Test
    public void testGetRelativePathFileToFile() {
        String target = "C:\\Windows\\Boot\\Fonts\\chs_boot.ttf";
        String base = "C:\\Windows\\Speech\\Common\\sapisvr.exe";

        String relPath = FilePaths.relativePath(target, base, "\\");
        assertEquals("..\\..\\Boot\\Fonts\\chs_boot.ttf", relPath);
    }

    @Test
    public void testGetRelativePathDirectoryToFile() {
        String target = "C:\\Windows\\Boot\\Fonts\\chs_boot.ttf";
        String base = "C:\\Windows\\Speech\\Common\\";

        String relPath = FilePaths.relativePath(target, base, "\\");
        assertEquals("..\\..\\Boot\\Fonts\\chs_boot.ttf", relPath);
    }

    @Test
    public void testGetRelativePathFileToDirectory() {
        String target = "C:\\Windows\\Boot\\Fonts";
        String base = "C:\\Windows\\Speech\\Common\\foo.txt";

        String relPath = FilePaths.relativePath(target, base, "\\");
        assertEquals("..\\..\\Boot\\Fonts", relPath);
    }

    @Test
    public void testGetRelativePathDirectoryToDirectory() {
        String target = "C:\\Windows\\Boot\\";
        String base = "C:\\Windows\\Speech\\Common\\";
        String expected = "..\\..\\Boot";

        String relPath = FilePaths.relativePath(target, base, "\\");
        assertEquals(expected, relPath);
    }

    @Test
    public void testGetRelativePathDifferentDriveLetters() {
        String target = "D:\\sources\\recovery\\RecEnv.exe";
        String base = "C:\\Java\\workspace\\AcceptanceTests\\Standard test data\\geo\\";

        try {
            FilePaths.relativePath(target, base, "\\");
            fail();

        } catch (FilePaths.PathResolutionException ex) {
            // expected exception
        }
    }

    @Test
    public void testTargetAndBaseAreIdentical() {
        String target = "C:\\Java\\workspace\\AcceptanceTests\\Standard test data\\geo";
        String base = "C:\\Java\\workspace\\AcceptanceTests\\Standard test data\\geo";

        assertEquals(".", FilePaths.relativePath(target, base, "\\"));
    }

    @Test
    public void testTargetAndBaseAreIdentical2() {
        String target = "C:\\Java\\workspace\\AcceptanceTests\\Standard test data\\geo";
        String base = "C:\\Java\\workspace\\AcceptanceTests\\Standard test data\\geo";

        assertEquals(".", FilePaths.relativePath(target, base, "\\"));
    }
}
