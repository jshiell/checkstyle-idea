package org.infernus.idea.checkstyle.model;

import org.jetbrains.annotations.NotNull;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;

/**
 * A configuration location located on a HTTP server for which we wish to ignore any SSL
 * errors. Caveat emptor.
 */
public class InsecureHTTPURLConfigurationLocation extends HTTPURLConfigurationLocation {

    public InsecureHTTPURLConfigurationLocation() {
        super(ConfigurationType.INSECURE_HTTP_URL);
    }

    @NotNull
    @Override
    protected InputStream resolveFile() throws IOException {
        TrustManager[] trustAllCerts = new TrustManager[]{new AllTrustingTrustManager()};

        try {
            final SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception ignored) {
            // we care not for security
        }

        return super.resolveFile();
    }

    private static class AllTrustingTrustManager implements X509TrustManager {
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[] {};
        }

        public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
        }

        public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
        }
    }
}
