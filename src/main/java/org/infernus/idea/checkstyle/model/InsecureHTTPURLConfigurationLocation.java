package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
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

    public InsecureHTTPURLConfigurationLocation(@NotNull final Project project,
                                 @NotNull final String id) {
        super(id, ConfigurationType.INSECURE_HTTP_URL, project);
    }

    @NotNull
    @Override
    protected InputStream resolveFile(@NotNull ClassLoader checkstyleClassLoader) throws IOException {
        TrustManager[] trustAllCerts = new TrustManager[]{new AllTrustingTrustManager()};

        try {
            final SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception ignored) {
            // we care not for security
        }

        return super.resolveFile(checkstyleClassLoader);
    }

    private static class AllTrustingTrustManager implements X509TrustManager {
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
