package org.infernus.idea.checkstyle.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FileUtilsTest {

    @Test
    public void testGetRelativePathsUnix() {
        assertEquals("stuff/xyz.dat", FileUtils.getRelativePath("/var/data/stuff/xyz.dat", "/var/data/", "/"));
        assertEquals("../../b/c", FileUtils.getRelativePath("/a/b/c", "/a/x/y/", "/"));
        assertEquals("../../b/c", FileUtils.getRelativePath("/m/n/o/a/b/c", "/m/n/o/a/x/y/", "/"));
    }

    @Test
    public void testGetRelativePathFileToFile() {
        String target = "C:\\Windows\\Boot\\Fonts\\chs_boot.ttf";
        String base = "C:\\Windows\\Speech\\Common\\sapisvr.exe";

        String relPath = FileUtils.getRelativePath(target, base, "\\");
        assertEquals("..\\..\\Boot\\Fonts\\chs_boot.ttf", relPath);
    }

    @Test
    public void testGetRelativePathDirectoryToFile() {
        String target = "C:\\Windows\\Boot\\Fonts\\chs_boot.ttf";
        String base = "C:\\Windows\\Speech\\Common\\";

        String relPath = FileUtils.getRelativePath(target, base, "\\");
        assertEquals("..\\..\\Boot\\Fonts\\chs_boot.ttf", relPath);
    }

    @Test
    public void testGetRelativePathFileToDirectory() {
        String target = "C:\\Windows\\Boot\\Fonts";
        String base = "C:\\Windows\\Speech\\Common\\foo.txt";

        String relPath = FileUtils.getRelativePath(target, base, "\\");
        assertEquals("..\\..\\Boot\\Fonts", relPath);
    }

    @Test
    public void testGetRelativePathDirectoryToDirectory() {
        String target = "C:\\Windows\\Boot\\";
        String base = "C:\\Windows\\Speech\\Common\\";
        String expected = "..\\..\\Boot";

        String relPath = FileUtils.getRelativePath(target, base, "\\");
        assertEquals(expected, relPath);
    }

    @Test
    public void testGetRelativePathDifferentDriveLetters() {
        String target = "D:\\sources\\recovery\\RecEnv.exe";
        String base = "C:\\Java\\workspace\\AcceptanceTests\\Standard test data\\geo\\";

        try {
            FileUtils.getRelativePath(target, base, "\\");
            fail();

        } catch (FileUtils.PathResolutionException ex) {
            // expected exception
        }
    }

    @Test
    public void testTargetAndBaseAreIdentical() {
        String target = "C:\\Java\\workspace\\AcceptanceTests\\Standard test data\\geo";
        String base = "C:\\Java\\workspace\\AcceptanceTests\\Standard test data\\geo";

        assertEquals(".", FileUtils.getRelativePath(target, base, "\\"));
    }

    @Test
    public void testTargetAndBaseAreIdentical2() {
        String target = "C:\\Java\\workspace\\AcceptanceTests\\Standard test data\\geo";
        String base = "C:\\Java\\workspace\\AcceptanceTests\\Standard test data\\geo";

        assertEquals(".", FileUtils.getRelativePath(null, base, "\\"));
    }
}
