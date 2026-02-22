package org.infernus.idea.checkstyle.service;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;


public final class FileUtil {

    private FileUtil() {
        super();
    }


    public static String readFile(@NotNull final String filename) throws IOException, URISyntaxException {
        URL url = FileUtil.class.getResource(filename);
        if (url == null) {
            url = Thread.currentThread().getContextClassLoader().getResource(filename);
        }
        assertNotNull(url, "File not found: " + filename);
        return Files.readString(Paths.get(url.toURI()));
    }
}
