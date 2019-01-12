package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;

import static org.infernus.idea.checkstyle.util.Streams.readContentOf;

/**
 * A configuration file accessible via a HTTP URL.
 */
public class HTTPURLConfigurationLocation extends ConfigurationLocation {

    private static final Logger LOG = Logger.getInstance(HTTPURLConfigurationLocation.class);

    private static final int CONTENT_CACHE_SECONDS = 2;
    private static final int ONE_SECOND = 1000;
    private static final int HTTP_TIMEOUT_IN_MS = 5000;

    private byte[] cachedContent;
    private long cacheExpiry;

    HTTPURLConfigurationLocation(@NotNull final Project project) {
        super(ConfigurationType.HTTP_URL, project);
    }

    HTTPURLConfigurationLocation(final ConfigurationType configurationType,
                                 @NotNull final Project project) {
        super(configurationType, project);
    }

    @NotNull
    protected InputStream resolveFile() throws IOException {
        if (cachedContent != null && cacheExpiry > System.currentTimeMillis()) {
            return new ByteArrayInputStream(cachedContent);
        }

        try {
            cachedContent = readContentOf(streamFrom(connectionTo(getLocation())));
            cacheExpiry = System.currentTimeMillis() + (CONTENT_CACHE_SECONDS * ONE_SECOND);
            return new ByteArrayInputStream(cachedContent);

        } catch (IOException e) {
            LOG.info("Couldn't read URL: " + getLocation(), e);
            cachedContent = null;
            cacheExpiry = 0;
            throw e;
        }
    }

    @NotNull
    URLConnection connectionTo(final String location) throws IOException {
        final URL url = new URL(location);

        final URLConnection urlConnection = url.openConnection();
        urlConnection.setConnectTimeout(HTTP_TIMEOUT_IN_MS);
        urlConnection.setReadTimeout(HTTP_TIMEOUT_IN_MS);
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(false);
        urlConnection.setAllowUserInteraction(false);

        return withBasicAuth(url, urlConnection);
    }

    private URLConnection withBasicAuth(final URL url, final URLConnection urlConnection) {
        if (url.getUserInfo() != null) {
            urlConnection.setRequestProperty("Authorization",
                    "Basic " + Base64.getEncoder().encodeToString(url.getUserInfo().getBytes()));
        }
        return urlConnection;
    }

    private InputStream streamFrom(final URLConnection urlConnection) throws IOException {
        urlConnection.connect();
        return new BufferedInputStream(urlConnection.getInputStream());
    }

    @Override
    public Object clone() {
        return cloneCommonPropertiesTo(new HTTPURLConfigurationLocation(getProject()));
    }
}
