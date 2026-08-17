package org.infernus.idea.checkstyle.service;

import com.intellij.openapi.vfs.VirtualFile;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.service.RulesContainer.ConfigurationLocationRulesContainer;
import org.infernus.idea.checkstyle.service.RulesContainer.ContentRulesContainer;
import org.infernus.idea.checkstyle.service.RulesContainer.VirtualFileRulesContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RulesContainerTest {

    // --- ContentRulesContainer ---

    @Test
    void contentContainerFilePathIsNull() {
        RulesContainer container = new ContentRulesContainer("content");
        assertThat(container.filePath(), is(nullValue()));
    }

    @Test
    void contentContainerInputStreamContainsContent() throws IOException {
        String content = "<module name=\"Checker\"/>";
        RulesContainer container = new ContentRulesContainer(content);

        try (InputStream is = container.inputStream(getClass().getClassLoader())) {
            String actual = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(actual, is(content));
        }
    }

    @Test
    void contentContainerInputStreamIsUtf8Encoded() throws IOException {
        String content = "UTF-8 content: \u00e9\u00e0\u00fc";
        RulesContainer container = new ContentRulesContainer(content);

        try (InputStream is = container.inputStream(getClass().getClassLoader())) {
            String actual = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(actual, is(content));
        }
    }

    @Test
    void contentContainerResolveAssociatedFileReturnsNull() throws IOException {
        RulesContainer container = new ContentRulesContainer("content");
        String result = container.resolveAssociatedFile("some-file.xml", null, getClass().getClassLoader());
        assertThat(result, is(nullValue()));
    }

    @Test
    void contentContainerBaseUriIsNull() {
        RulesContainer container = new ContentRulesContainer("content");
        assertThat(container.baseUri(), is(nullValue()));
    }

    // --- ConfigurationLocationRulesContainer ---

    @Test
    void configurationLocationContainerBaseUriIsThatOfTheLocation() {
        ConfigurationLocation location = mock(ConfigurationLocation.class);
        when(location.baseUri()).thenReturn("file:/a/path/to/checkstyle.xml");

        RulesContainer container = new ConfigurationLocationRulesContainer(location);

        assertThat(container.baseUri(), is("file:/a/path/to/checkstyle.xml"));
    }

    // --- VirtualFileRulesContainer ---

    @Test
    void virtualFileContainerBaseUriIsTheFilesUri(@TempDir final Path tempDir) throws IOException {
        Path configFile = Files.createFile(tempDir.resolve("checkstyle.xml"));
        VirtualFile virtualFile = mock(VirtualFile.class);
        when(virtualFile.isInLocalFileSystem()).thenReturn(true);
        when(virtualFile.getUrl()).thenReturn("file://" + configFile);

        RulesContainer container = new VirtualFileRulesContainer(virtualFile);

        assertThat(container.baseUri(), is(configFile.toFile().toURI().toString()));
    }

    @Test
    void virtualFileContainerBaseUriIsNullOutsideTheLocalFileSystem() {
        VirtualFile virtualFile = mock(VirtualFile.class);
        when(virtualFile.isInLocalFileSystem()).thenReturn(false);

        RulesContainer container = new VirtualFileRulesContainer(virtualFile);

        assertThat(container.baseUri(), is(nullValue()));
    }
}
