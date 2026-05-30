package org.infernus.idea.checkstyle.util;

import org.infernus.idea.checkstyle.TestHelper;
import org.infernus.idea.checkstyle.StringConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import javax.xml.stream.XMLStreamException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for CheckStyleEntityResolver — in particular the SSRF fix that returns
 * null for unknown non-file system IDs.
 */
public class CheckStyleEntityResolverTest {

    private CheckStyleEntityResolver underTest;

    @BeforeEach
    public void setUp() {
        final ConfigurationLocation location =
                new StringConfigurationLocation("<module/>", TestHelper.mockProject());
        underTest = new CheckStyleEntityResolver(location, getClass().getClassLoader());
    }

    @Test
    public void unknownHttpSystemIdReturnsNull() throws Exception {
        final InputSource result = underTest.resolveEntity(null, "http://evil.example.com/malicious.dtd");
        assertThat("Expected null for unknown http system ID to prevent SSRF", result, nullValue());
    }

    @Test
    public void unknownHttpsSystemIdReturnsNull() throws Exception {
        final InputSource result = underTest.resolveEntity(null, "https://evil.example.com/malicious.dtd");
        assertThat("Expected null for unknown https system ID to prevent SSRF", result, nullValue());
    }

    @Test
    public void knownCheckstyleDtdIsResolved() throws Exception {
        final InputSource result = underTest.resolveEntity(
                "-//Puppy Crawl//DTD Check Configuration 1.3//EN",
                "http://www.puppycrawl.com/dtds/configuration_1_3.dtd");
        assertThat("Known DTD should be resolved from classpath", result, notNullValue());
    }

    @Test
    public void nullSystemIdReturnsNull() throws Exception {
        final InputSource result = underTest.resolveEntity(null, null);
        assertThat(result, nullValue());
    }

    @Test
    public void xmlResolverDelegatesCorrectly() throws XMLStreamException {
        // Unknown non-file URI should not cause an exception, and should return null
        final Object result = underTest.resolveEntity(null, "http://evil.example.com/dtd", null, null);
        assertThat(result, nullValue());
    }
}
