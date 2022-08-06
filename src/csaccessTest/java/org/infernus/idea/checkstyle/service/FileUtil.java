package org.infernus.idea.checkstyle.service;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;


public final class FileUtil {

    private FileUtil() {
        super();
    }


    public static String readFile(@NotNull final String filename) throws IOException, URISyntaxException {
        URL url = FileUtil.class.getResource(filename);
        if (url == null) {
            url = Thread.currentThread().getContextClassLoader().getResource(filename);
        }
        Assert.assertNotNull("File not found: " + filename, url);
        return Files.readString(Paths.get(url.toURI()));
    }
}
