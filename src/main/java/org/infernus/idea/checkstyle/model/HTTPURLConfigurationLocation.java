package org.infernus.idea.checkstyle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * A configuration file accessible via a HTTP URL.
 */
public class HTTPURLConfigurationLocation extends ConfigurationLocation {

    private static final Log LOG = LogFactory.getLog(HTTPURLConfigurationLocation.class);

    private File tempFile;

    /**
     * Create a new URL configuration.
     */
    HTTPURLConfigurationLocation() {
        super(ConfigurationType.HTTP_URL);
    }

    HTTPURLConfigurationLocation(final ConfigurationType configurationType) {
        super(configurationType);
    }

    @NotNull
    protected InputStream resolveFile() throws IOException {
        try {
            return new FileInputStream(writeFileTo(connectionTo(getLocation()), temporaryFile()));

        } catch (IOException e) {
            LOG.error("Couldn't read URL: " + getLocation(), e);
            throw e;
        }
    }

    @NotNull
    private URLConnection connectionTo(final String location) throws IOException {
        final URL url = new URL(location);
        final URLConnection urlConnection = url.openConnection();
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

    private File writeFileTo(final URLConnection urlConnection, final File destinationFile) throws IOException {
        urlConnection.connect();

        try (Writer writer = new BufferedWriter(new FileWriter(destinationFile));
             Reader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
            int readChar;
            while ((readChar = reader.read()) != -1) {
                writer.write(readChar);
            }

            writer.flush();

            return destinationFile;
        }
    }

    @NotNull
    private File temporaryFile() throws IOException {
        synchronized (this) {
            if (tempFile == null) {
                tempFile = File.createTempFile("checkStyle", ".xml");
                tempFile.deleteOnExit();
            }
            return tempFile;
        }
    }

    @Override
    public Object clone() {
        return cloneCommonPropertiesTo(new HTTPURLConfigurationLocation());
    }
}
