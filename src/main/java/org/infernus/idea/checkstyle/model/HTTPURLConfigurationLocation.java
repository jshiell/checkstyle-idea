package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;

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

    HTTPURLConfigurationLocation() {
        super(ConfigurationType.HTTP_URL);
    }

    HTTPURLConfigurationLocation(final ConfigurationType configurationType) {
        super(configurationType);
    }

    @NotNull
    protected InputStream resolveFile() throws IOException {
        if (cachedContent != null && cacheExpiry > System.currentTimeMillis()) {
            return new ByteArrayInputStream(cachedContent);
        }

        try {
            cachedContent = asBytes(connectionTo(getLocation()));
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

        addBasicAuth(url, urlConnection);

        return urlConnection;
    }

    private void addBasicAuth(final URL url, final URLConnection urlConnection) {
        if (url.getUserInfo() != null) {
            urlConnection.setRequestProperty("Authorization",
                    "Basic " + DatatypeConverter.printBase64Binary(url.getUserInfo().getBytes()));
        }
    }

    private byte[] asBytes(final URLConnection urlConnection) throws IOException {
        urlConnection.connect();

        final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(responseBody));
             Reader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
            int readChar;
            while ((readChar = reader.read()) != -1) {
                writer.write(readChar);
            }

            writer.flush();

            return responseBody.toByteArray();
        }
    }

    @Override
    public Object clone() {
        return cloneCommonPropertiesTo(new HTTPURLConfigurationLocation());
    }
}
