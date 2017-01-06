package org.infernus.idea.checkstyle.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;


public final class FileUtil
{
    private FileUtil() {
        super();
    }


    public static String readFile(@NotNull final String pFilename) throws IOException, URISyntaxException {
        URL url = FileUtil.class.getResource(pFilename);
        if (url == null) {
            url = Thread.currentThread().getContextClassLoader().getResource(pFilename);
        }
        Assert.assertNotNull("File not found: " + pFilename, url);
        return new String(Files.readAllBytes(Paths.get(url.toURI())), StandardCharsets.UTF_8);
    }
}
