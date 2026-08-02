package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import static org.infernus.idea.checkstyle.util.Streams.readContentOf;

/**
 * A configuration file accessible via a HTTP URL.
 */
public class HTTPURLConfigurationLocation extends ConfigurationLocation {

    private static final Logger LOG = Logger.getInstance(HTTPURLConfigurationLocation.class);

    static final int CONTENT_CACHE_SECONDS = 2;
    static final int FAILURE_CACHE_SECONDS = 60;
    static final int ONE_SECOND = 1000;
    private static final int HTTP_TIMEOUT_IN_MS = 5000;
    private static final int MAX_REDIRECTS = 5;

    private byte[] cachedContent;
    private long cacheExpiry;
    private long failureExpiry;
    private IOException lastFailure;
    private String etag;
    private String lastModified;
    private String validatedLocation;

    HTTPURLConfigurationLocation(@NotNull final Project project,
                                 @NotNull final String id) {
        super(id, ConfigurationType.HTTP_URL, project);
    }

    HTTPURLConfigurationLocation(@NotNull final String id,
                                 @NotNull final ConfigurationType configurationType,
                                 @NotNull final Project project) {
        super(id, configurationType, project);
    }

    @NotNull
    protected InputStream resolveFile(@NotNull final ClassLoader checkstyleClassLoader) throws IOException {
        if (cachedContent != null && cacheExpiry > now()) {
            return new ByteArrayInputStream(cachedContent);
        }

        if (failureExpiry > now()) {
            if (cachedContent != null && isConnectionFailure(lastFailure)) {
                return new ByteArrayInputStream(cachedContent);
            }
            throw new IOException("Skipping unavailable HTTP configuration (in cooldown): " + redactedLocation());
        }

        try {
            final FetchResult result = fetchFrom(connectionTo(getLocation()));
            if (result.statusCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                if (cachedContent == null) {
                    // we never send a conditional request without cached content, so this can only be a
                    // misbehaving proxy. Thrown from within the try so that it is logged and puts the
                    // location into cooldown like any other bad response.
                    throw new IOException("Received an unexpected 304 for " + redactedLocation());
                }
                markAsFresh();
                return new ByteArrayInputStream(cachedContent);
            }

            cachedContent = result.body();
            etag = result.etag();
            lastModified = result.lastModified();
            validatedLocation = result.effectiveUrl();
            markAsFresh();
            return new ByteArrayInputStream(cachedContent);

        } catch (IOException e) {
            LOG.warn("Couldn't read URL: " + redactedLocation(), e);
            cacheExpiry = 0;
            failureExpiry = now() + (FAILURE_CACHE_SECONDS * ONE_SECOND);
            lastFailure = e;

            if (cachedContent != null && isConnectionFailure(e)) {
                return new ByteArrayInputStream(cachedContent);
            }
            throw e;
        }
    }

    /**
     * Is this failure one where the server could not be reached at all, as opposed to one where it
     * answered? Only the former is a candidate for serving the last known good content, as a server
     * that answers with an error may be telling us the configuration has been moved or removed.
     */
    private boolean isConnectionFailure(final IOException e) {
        return e instanceof ConnectException
                || e instanceof UnknownHostException
                || e instanceof SocketTimeoutException
                || e instanceof NoRouteToHostException
                || e instanceof SSLException;
    }

    @Override
    public synchronized void reset() {
        super.reset();

        // a forced reload must genuinely contact the server, and must not be defeated by the failure
        // cooldown. The cached content and its validators are kept, so if nothing has changed this
        // costs us only a conditional request.
        cacheExpiry = 0;
        failureExpiry = 0;
        lastFailure = null;
    }

    @Override
    public synchronized void setLocation(final String location) {
        final String previousLocation = getRawLocation();
        super.setLocation(location);

        // only once the new location has been accepted, and only if it is genuinely different: the
        // settings dialogue re-sets the location on every OK, even when it was never edited.
        if (!location.equals(previousLocation)) {
            discardCachedContent();
        }
    }

    private void discardCachedContent() {
        cachedContent = null;
        etag = null;
        lastModified = null;
        validatedLocation = null;
        cacheExpiry = 0;
        failureExpiry = 0;
        lastFailure = null;
    }

    private void markAsFresh() {
        cacheExpiry = now() + (CONTENT_CACHE_SECONDS * ONE_SECOND);
        failureExpiry = 0;
        lastFailure = null;
    }

    /**
     * The current time in milliseconds.
     * <p>
     * Package-private (rather than private) to allow the cache timings to be driven by unit tests.
     */
    long now() {
        return System.currentTimeMillis();
    }

    @NotNull
    URLConnection connectionTo(final String location) throws IOException {
        final URL url = URI.create(location).toURL();

        final URLConnection urlConnection = url.openConnection();
        urlConnection.setConnectTimeout(HTTP_TIMEOUT_IN_MS);
        urlConnection.setReadTimeout(HTTP_TIMEOUT_IN_MS);
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(false);
        urlConnection.setAllowUserInteraction(false);

        if (urlConnection instanceof HttpURLConnection httpURLConnection) {
            httpURLConnection.setInstanceFollowRedirects(false);
        }

        return withBasicAuth(url, urlConnection);
    }

    private URLConnection withBasicAuth(final URL url, final URLConnection urlConnection) {
        if (url.getUserInfo() != null) {
            urlConnection.setRequestProperty("Authorization",
                    "Basic " + Base64.getEncoder().encodeToString(
                            URLDecoder.decode(url.getUserInfo(), StandardCharsets.UTF_8).getBytes()));
        }
        return urlConnection;
    }

    private FetchResult fetchFrom(final URLConnection urlConnection) throws IOException {
        URLConnection current = withConditionalHeaders(urlConnection);
        for (int hops = 0; hops < MAX_REDIRECTS; hops++) {
            if (!(current instanceof HttpURLConnection httpConn)) {
                break;
            }
            httpConn.connect();
            final int status = httpConn.getResponseCode();
            if (status == 301 || status == 302 || status == 307 || status == 308) {
                final String newUrl = httpConn.getHeaderField("Location");
                httpConn.disconnect();
                if (newUrl == null) {
                    throw new IOException("Redirect response missing Location header");
                }
                current = withConditionalHeaders(connectionTo(newUrl));
            } else {
                return resultOf(httpConn, status);
            }
        }

        current.connect();
        if (current instanceof HttpURLConnection httpConn) {
            return resultOf(httpConn, httpConn.getResponseCode());
        }
        return contentOf(current, HttpURLConnection.HTTP_OK);
    }

    /**
     * Offers our stored validators back to the server, so that an unchanged file costs us a 304
     * rather than a full transfer. Applied per hop, and only to the hop that issued the validators:
     * the configured URL of a redirected resource never saw the ETag, so must not be sent it.
     */
    private URLConnection withConditionalHeaders(final URLConnection connection) {
        if (cachedContent != null && connection.getURL().toString().equals(validatedLocation)) {
            if (etag != null) {
                connection.setRequestProperty("If-None-Match", etag);
            }
            if (lastModified != null) {
                connection.setRequestProperty("If-Modified-Since", lastModified);
            }
        }
        return connection;
    }

    private FetchResult resultOf(final HttpURLConnection connection, final int status) throws IOException {
        if (status == HttpURLConnection.HTTP_NOT_MODIFIED) {
            // the body of a 304 must never be read: the JDK's behaviour for such a stream is not
            // reliably specified, and by definition there is nothing useful in it.
            connection.disconnect();
            return new FetchResult(status, null, null, null, null);
        }
        return contentOf(connection, status);
    }

    private FetchResult contentOf(final URLConnection connection, final int status) throws IOException {
        // for a non-2xx status getInputStream() throws rather than returning a stream, giving us the
        // JDK's own exception for the status - notably a FileNotFoundException for a 404.
        final byte[] body = readContentOf(new BufferedInputStream(connection.getInputStream()));
        return new FetchResult(status, body,
                connection.getHeaderField("ETag"),
                connection.getHeaderField("Last-Modified"),
                connection.getURL().toString());
    }

    /**
     * The outcome of a single fetch, after any redirects have been followed. A 304 carries no body
     * and no validators; {@code effectiveUrl} is the URL that actually served the content, which is
     * the URL any validators must be presented back to.
     */
    private record FetchResult(int statusCode,
                               byte[] body,
                               String etag,
                               String lastModified,
                               String effectiveUrl) {
    }

    @Override
    public Object clone() {
        return cloneCommonPropertiesTo(new HTTPURLConfigurationLocation(getProject(), getId()));
    }

    /**
     * Returns the location URL with any embedded credentials (userInfo) removed, to prevent
     * logging plaintext passwords to idea.log.
     * <p>
     * Package-private (rather than private) to allow direct unit testing.
     */
    String redactedLocation() {
        try {
            URI uri = new URI(getLocation());
            if (uri.getUserInfo() != null) {
                return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(),
                        uri.getPath(), uri.getQuery(), uri.getFragment()).toString();
            }
        } catch (URISyntaxException ignored) {
        }
        return getLocation();
    }
}
