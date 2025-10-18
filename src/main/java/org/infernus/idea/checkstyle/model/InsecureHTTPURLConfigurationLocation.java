package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

/**
 * A configuration location located on a HTTP server for which we wish to ignore any SSL
 * errors. Caveat emptor.
 */
public class InsecureHTTPURLConfigurationLocation extends HTTPURLConfigurationLocation {

    public InsecureHTTPURLConfigurationLocation(@NotNull final Project project,
                                 @NotNull final String id) {
        super(id, ConfigurationType.INSECURE_HTTP_URL, project);
    }

    @Override
    @NotNull URLConnection connectionTo(final String location) throws IOException {
        final URLConnection urlConnection = super.connectionTo(location);

        if (urlConnection instanceof HttpsURLConnection httpsURLConnection) {
            try {
                final TrustManager[] trustAllCerts = new TrustManager[]{new AllTrustingTrustManager()};
                final SSLContext sc = SSLContext.getInstance("TLSv1.3");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                httpsURLConnection.setSSLSocketFactory(sc.getSocketFactory());
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new IOException("Failed to set an insecure SSL socket factory", e);
            }
        }

        return urlConnection;
    }

    private static final class AllTrustingTrustManager implements X509TrustManager {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[] {};
        }

        @Override
        public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
        }

        @Override
        public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
        }
    }

    @Override
    public Object clone() {
        return cloneCommonPropertiesTo(new InsecureHTTPURLConfigurationLocation(getProject(), getId()));
    }
}
