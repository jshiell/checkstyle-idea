package org.infernus.idea.checkstyle.model;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.infernus.idea.checkstyle.TestHelper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.URLConnection;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.infernus.idea.checkstyle.model.HTTPURLConfigurationLocation.CONTENT_CACHE_SECONDS;
import static org.infernus.idea.checkstyle.model.HTTPURLConfigurationLocation.ONE_SECOND;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HTTPURLConfigurationLocationTest {

    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();

    private HttpServer httpServer;
    private int serverPort = -1;

    @BeforeEach
    public void startHttpServer() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/", new TestHandler());
        httpServer.setExecutor(null);
        httpServer.start();
        serverPort = httpServer.getAddress().getPort();
    }

    @AfterEach
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

    @Test
    public void aMissingRemoteFileThrowsAFileNotFoundException() {
        assertThrows(FileNotFoundException.class,
                () -> aLocationWithPath("/invalid").resolveFile(getClass().getClassLoader()));
    }

    @Test
    public void aTimeoutThrowsASocketTimeoutException() {
        assertThrows(SocketTimeoutException.class,
                () -> aTimingOutLocation().resolveFile(getClass().getClassLoader()));
    }

    @Test
    public void aFailedFetchIsNotRetriedWithinTheCooldownPeriod() throws IOException {
        final AtomicInteger connectionAttempts = new AtomicInteger(0);
        final HTTPURLConfigurationLocation location = new HTTPURLConfigurationLocation(TestHelper.mockProject(), UUID.randomUUID().toString()) {
            @Override
            URLConnection connectionTo(final String loc) throws IOException {
                connectionAttempts.incrementAndGet();
                throw new IOException("simulated failure");
            }
        };
        location.setDescription("aFailingLocation");
        location.setLocation("http://localhost:1/does-not-exist");

        assertThrows(IOException.class, () -> location.resolveFile(getClass().getClassLoader()));
        assertThrows(IOException.class, () -> location.resolveFile(getClass().getClassLoader()));

        assertThat("second call within cooldown should not retry connection", connectionAttempts.get(), is(1));
    }

    /**
     * Note: this exercises the fake clock rather than wall-clock time. It could in theory pass
     * spuriously on a machine slow enough that more than {@code CONTENT_CACHE_SECONDS} of real time
     * elapsed between the first and second calls.
     */
    @Test
    public void cachedContentExpiresOnceTheCacheTtlElapses() throws IOException {
        final FakeClockLocation location = aFakeClockLocationWithPath("/valid");

        location.resolveFile(getClass().getClassLoader());
        location.resolveFile(getClass().getClassLoader());

        assertThat("a call within the TTL should be served from cache", requestsTo("/valid"), is(1));

        location.advanceBy((CONTENT_CACHE_SECONDS * ONE_SECOND) + 1);
        location.resolveFile(getClass().getClassLoader());

        assertThat("a call after the TTL should hit the server", requestsTo("/valid"), is(2));
    }

    @Test
    public void staleCachedContentIsServedWhenTheServerIsUnreachable() throws IOException {
        final FakeClockLocation location = aFakeClockLocationWithPath("/valid");
        location.resolveFile(getClass().getClassLoader());

        location.failConnectionsWith(new ConnectException("simulated network outage"));
        location.advanceBy((CONTENT_CACHE_SECONDS * ONE_SECOND) + 1);

        assertThat(toString(location.resolveFile(getClass().getClassLoader())), is("A test response"));
    }

    private int requestsTo(final String path) {
        return requestCounts.getOrDefault(path, new AtomicInteger(0)).get();
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
    private FakeClockLocation aFakeClockLocationWithPath(final String path) {
        final FakeClockLocation location = new FakeClockLocation();
        location.setDescription("aTestLocation");
        location.setLocation(urlOf(path));
        return location;
    }

    @NotNull
    private String urlOf(final String path) {
        return format("http://localhost:%s%s", serverPort, path);
    }

    @NotNull
    private HTTPURLConfigurationLocation aTimingOutLocation() {
        final TimingOutHTTPURLConfigurationLocation location = new TimingOutHTTPURLConfigurationLocation();
        location.setDescription("aTimingOutTestLocation");
        location.setLocation(format("http://localhost:%s%s", serverPort, "/delayed"));
        location.setNamedScope(TestHelper.NAMED_SCOPE);
        return location;
    }

    private final class TestHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange exch) throws IOException {
            final String path = exch.getRequestURI().getPath();
            requestCounts.computeIfAbsent(path, ignored -> new AtomicInteger(0)).incrementAndGet();

            String response;
            int status;
            switch (path) {
            case "/valid":
                response = "A test response";
                status = 200;
                break;
            case "/delayed":
                waitFor();
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

        private void waitFor() {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
    }

    /**
     * A location whose clock is driven by the test rather than by wall-clock time. The clock starts
     * at a nonzero value so that the initial zeroed expiry fields cannot compare as unexpired.
     */
    private static class FakeClockLocation extends HTTPURLConfigurationLocation {
        private final AtomicInteger connectionAttempts = new AtomicInteger(0);

        private long time = 1_000_000L;
        private IOException connectionFailure;

        FakeClockLocation() {
            super(TestHelper.mockProject(), UUID.randomUUID().toString());
        }

        @Override
        long now() {
            return time;
        }

        void advanceBy(final long millis) {
            time += millis;
        }

        @NotNull
        @Override
        URLConnection connectionTo(final String location) throws IOException {
            connectionAttempts.incrementAndGet();
            if (connectionFailure != null) {
                throw connectionFailure;
            }
            return super.connectionTo(location);
        }

        void failConnectionsWith(final IOException failure) {
            this.connectionFailure = failure;
        }

        int connectionAttempts() {
            return connectionAttempts.get();
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
