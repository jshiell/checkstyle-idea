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
        Reader reader = null;
        Writer writer = null;
        try {
            final URL url = new URL(getLocation());
            final URLConnection urlConnection = url.openConnection();
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(false);
            urlConnection.setAllowUserInteraction(false);

            if (url.getUserInfo() != null) {
                final String basicAuth = "Basic " + DatatypeConverter.printBase64Binary(url.getUserInfo().getBytes());
                urlConnection.setRequestProperty("Authorization", basicAuth);
            }

            final File tempFile = File.createTempFile("checkStyle", ".xml");
            tempFile.deleteOnExit();
            writer = new BufferedWriter(new FileWriter(tempFile));

            urlConnection.connect();
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            int readChar;
            while ((readChar = reader.read()) != -1) {
                writer.write(readChar);
            }

            writer.flush();
            return new FileInputStream(tempFile);

        } catch (IOException e) {
            LOG.error("Couldn't read URL: " + getLocation(), e);
            throw e;

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    @Override
    public Object clone() {
        return cloneCommonPropertiesTo(new HTTPURLConfigurationLocation());
    }
}
