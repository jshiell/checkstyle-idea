package org.infernus.idea.checkstyle.model;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.infernus.idea.checkstyle.TestHelper;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.URLConnection;
import java.util.Scanner;
import java.util.UUID;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class HTTPURLConfigurationLocationTest {

    private HttpServer httpServer;
    private int serverPort = -1;

    @Before
    public void startHttpServer() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/", new TestHandler());
        httpServer.setExecutor(null);
        httpServer.start();
        serverPort = httpServer.getAddress().getPort();
    }

    @After
    public void stopHttpServer() {
        httpServer.stop(0);
    }

    @Test
    public void aRemoteFileCanBeFetched() throws IOException {
        final InputStream stream = aLocationWithPath("/valid").resolveFile(getClass().getClassLoader());

        assertThat(toString(stream), is("A test response"));
    }

    @Test
    public void aRemoteFileCanBeFetchedViaARedirect() throws IOException {
        final InputStream stream = aLocationWithPath("/redirect").resolveFile(getClass().getClassLoader());

        assertThat(toString(stream), is("A test response"));
    }

    @Test(expected = FileNotFoundException.class)
    public void aMissingRemoteFileThrowsAFileNotFoundException() throws IOException {
        aLocationWithPath("/invalid").resolveFile(getClass().getClassLoader());
    }

    @Test(expected = SocketTimeoutException.class)
    public void aTimeoutThrowsASocketTimeoutException() throws IOException {
        aTimingOutLocation().resolveFile(getClass().getClassLoader());
    }

    private String toString(final InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    @NotNull
    private HTTPURLConfigurationLocation aLocationWithPath(final String path) {
        final HTTPURLConfigurationLocation location = new HTTPURLConfigurationLocation(TestHelper.mockProject(), UUID.randomUUID().toString());
        location.setDescription("aTestLocation");
        location.setLocation(format("http://localhost:%s%s", serverPort, path));
        return location;
    }

    @NotNull
    private HTTPURLConfigurationLocation aTimingOutLocation() {
        final TimingOutHTTPURLConfigurationLocation location = new TimingOutHTTPURLConfigurationLocation();
        location.setDescription("aTimingOutTestLocation");
        location.setLocation(format("http://localhost:%s%s", serverPort, "/delayed"));
        location.setNamedScope(TestHelper.NAMED_SCOPE);
        return location;
    }

    private class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exch) throws IOException {
            String response;
            int status;
            switch (exch.getRequestURI().getPath()) {
            case "/valid":
                response = "A test response";
                status = 200;
                break;
            case "/delayed":
                waitFor(100);
                response = "A delayed test response";
                status = 200;
                break;
            case "/redirect":
                response = "A redirect";
                status = 301;
                exch.getResponseHeaders().add("Location", format("http://localhost:%s/valid", serverPort));
                break;
            default:
                response = "";
                status = 404;
            }

            exch.sendResponseHeaders(status, response.length());
            OutputStream os = exch.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        private void waitFor(final int millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private static class TimingOutHTTPURLConfigurationLocation extends HTTPURLConfigurationLocation {
        TimingOutHTTPURLConfigurationLocation() {
            super(TestHelper.mockProject(), UUID.randomUUID().toString());
        }

        @NotNull
        @Override
        URLConnection connectionTo(final String location) throws IOException {
            final URLConnection urlConnection = super.connectionTo(location);
            urlConnection.setConnectTimeout(1);
            urlConnection.setReadTimeout(1);
            return urlConnection;
        }
    }


}
