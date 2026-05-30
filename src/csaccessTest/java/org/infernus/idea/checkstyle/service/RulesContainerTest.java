package org.infernus.idea.checkstyle.service;

import org.infernus.idea.checkstyle.service.RulesContainer.ContentRulesContainer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

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
}
