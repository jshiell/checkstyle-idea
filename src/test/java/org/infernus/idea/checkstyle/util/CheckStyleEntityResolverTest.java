package org.infernus.idea.checkstyle.util;

import com.sun.net.httpserver.HttpServer;
import org.infernus.idea.checkstyle.TestHelper;
import org.infernus.idea.checkstyle.StringConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for CheckStyleEntityResolver — in particular that unknown remote entities
 * are neutered rather than fetched.
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
    public void unknownHttpSystemIdResolvesToAnEmptySource() throws Exception {
        final InputSource result = underTest.resolveEntity(null, "http://evil.example.com/malicious.dtd");
        assertThat("Expected an empty source for an unknown http system ID", contentOf(result), is(""));
    }

    @Test
    public void unknownHttpsSystemIdResolvesToAnEmptySource() throws Exception {
        final InputSource result = underTest.resolveEntity(null, "https://evil.example.com/malicious.dtd");
        assertThat("Expected an empty source for an unknown https system ID", contentOf(result), is(""));
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
    public void xmlResolverDelegatesCorrectly() throws Exception {
        final Object result = underTest.resolveEntity(null, "http://evil.example.com/dtd", null, null);
        assertThat(contentOf((InputSource) result), is(""));
    }

    @Test
    public void aRemoteEntityIsNeverFetchedDuringAParse() throws Exception {
        assertThat("The parser must not fetch remote entities",
                requestsWhileParsingAnEntityFrom("/evil.xml"), is(0));
    }

    @Test
    public void aRemoteEntityWhoseUrlCannotBeParsedIsNeverFetchedDuringAParse() throws Exception {
        // a system ID that java.net.URI rejects must still be blocked, rather than handed back to the parser
        assertThat("The parser must not fetch remote entities with unparseable URLs",
                requestsWhileParsingAnEntityFrom("/evil file.xml"), is(0));
    }

    private int requestsWhileParsingAnEntityFrom(final String path) throws Exception {
        final AtomicInteger requestCount = new AtomicInteger();
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestCount.incrementAndGet();
            final byte[] body = "<!-- pwned -->".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try {
            final String config = """
                    <!DOCTYPE module [
                      <!ENTITY remoteInclude SYSTEM "http://127.0.0.1:%d%s">
                    ]>
                    <module name="Checker">&remoteInclude;</module>"""
                    .formatted(server.getAddress().getPort(), path);

            parseWithExternalEntitiesEnabled(config);

            return requestCount.get();
        } finally {
            server.stop(0);
        }
    }

    private void parseWithExternalEntitiesEnabled(final String config) throws Exception {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", true);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true);

        final SAXParser parser = factory.newSAXParser();
        parser.getXMLReader().setEntityResolver(underTest);
        parser.getXMLReader().parse(new InputSource(
                new ByteArrayInputStream(config.getBytes(StandardCharsets.UTF_8))));
    }

    private String contentOf(final InputSource inputSource) throws Exception {
        assertThat("A source was expected", inputSource, notNullValue());

        final Reader reader = inputSource.getCharacterStream();
        assertThat("A character stream was expected", reader, notNullValue());

        final StringBuilder content = new StringBuilder();
        final char[] buffer = new char[64];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            content.append(buffer, 0, read);
        }
        return content.toString();
    }
}
