package org.infernus.idea.checkstyle.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Arrays.asList;

/**
 * Entity resolver for CheckStyle DTDs.
 */
public class CheckStyleEntityResolver implements EntityResolver {

    private static final Log LOG = LogFactory.getLog(CheckStyleEntityResolver.class);

    private static final Map<DTDKey, String> DTD_MAP = new HashMap<>();

    static {
        for (String host : asList("www.puppycrawl.com", "checkstyle.sourceforge.net", "checkstyle.org")) {
            for (String protocol : asList("http", "https")) {
                DTD_MAP.put(new DTDKey(
                                "-//Puppy Crawl//DTD Check Configuration 1.0//EN",
                                format("%s://%s/dtds/configuration_1_0.dtd", protocol, host)),
                        "/dtd/configuration_1_0.dtd");
                DTD_MAP.put(new DTDKey(
                                "-//Puppy Crawl//DTD Check Configuration 1.1//EN",
                                format("%s://%s/dtds/configuration_1_1.dtd", protocol, host)),
                        "/dtd/configuration_1_1.dtd");
                DTD_MAP.put(new DTDKey(
                                "-//Puppy Crawl//DTD Check Configuration 1.2//EN",
                                format("%s://%s/dtds/configuration_1_2.dtd", protocol, host)),
                        "/dtd/configuration_1_2.dtd");
                DTD_MAP.put(new DTDKey(
                                "-//Puppy Crawl//DTD Check Configuration 1.3//EN",
                                format("%s://%s/dtds/configuration_1_3.dtd", protocol, host)),
                        "/dtd/configuration_1_3.dtd");
                DTD_MAP.put(new DTDKey(
                                "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN",
                                format("%s://%s/dtds/configuration_1_3.dtd", protocol, host)),
                        "/dtd/configuration_1_3.dtd");

                DTD_MAP.put(new DTDKey("-//Puppy Crawl//DTD Import Control 1.0//EN",
                                format("%s://%s/dtds/import_control_1_0.dtd", protocol, host)),
                        "/dtd/import_control_1_0.dtd");
                DTD_MAP.put(new DTDKey("-//Puppy Crawl//DTD Import Control 1.1//EN",
                                format("%s://%s/dtds/import_control_1_1.dtd", protocol, host)),
                        "/dtd/import_control_1_1.dtd");
                DTD_MAP.put(new DTDKey("-//Puppy Crawl//DTD Import Control 1.2//EN",
                                format("%s://%s/dtds/import_control_1_2.dtd", protocol, host)),
                        "/dtd/import_control_1_2.dtd");
                DTD_MAP.put(new DTDKey("-//Puppy Crawl//DTD Import Control 1.3//EN",
                                format("%s://%s/dtds/import_control_1_3.dtd", protocol, host)),
                        "/dtd/import_control_1_3.dtd");
                DTD_MAP.put(new DTDKey("-//Checkstyle//DTD ImportControl Configuration 1.4//EN",
                                format("%s://%s/dtds/import_control_1_4.dtd", protocol, host)),
                        "/dtd/import_control_1_4.dtd");

                DTD_MAP.put(new DTDKey("-//Puppy Crawl//DTD Suppressions 1.0//EN",
                                format("%s://%s/dtds/suppressions_1_0.dtd", protocol, host)),
                        "/dtd/suppressions_1_0.dtd");
                DTD_MAP.put(new DTDKey("-//Puppy Crawl//DTD Suppressions 1.1//EN",
                                format("%s://%s/dtds/suppressions_1_1.dtd", protocol, host)),
                        "/dtd/suppressions_1_1.dtd");
                DTD_MAP.put(new DTDKey("-//Checkstyle//DTD SuppressionXpathFilter Experimental Configuration 1.1//EN",
                                format("%s://%s/dtds/suppressions_1_1_xpath_experimental.dtd", protocol, host)),
                        "/dtd/suppressions_1_1_xpath_experimental.dtd");
                DTD_MAP.put(new DTDKey("-//Puppy Crawl//DTD Suppressions 1.2//EN",
                                format("%s://%s/dtds/suppressions_1_2.dtd", protocol, host)),
                        "/dtd/suppressions_1_2.dtd");
                DTD_MAP.put(new DTDKey("-//Checkstyle//DTD SuppressionXpathFilter Experimental Configuration 1.2//EN",
                                format("%s://%s/dtds/suppressions_1_2_xpath_experimental.dtd", protocol, host)),
                        "/dtd/suppressions_1_2_xpath_experimental.dtd");

                DTD_MAP.put(new DTDKey("-//Puppy Crawl//DTD Package Names 1.0//EN",
                                format("%s://%s/dtds/packages_1_0.dtd", protocol, host)),
                        "/dtd/packages_1_0.dtd");
            }
        }
    }

    private final ConfigurationLocation configurationLocation;
    private final ClassLoader checkstyleClassLoader;

    public CheckStyleEntityResolver(final ConfigurationLocation configurationLocation,
                                    final ClassLoader checkstyleClassLoader) {
        this.configurationLocation = configurationLocation;
        this.checkstyleClassLoader = checkstyleClassLoader;
    }

    @Override
    public InputSource resolveEntity(final String publicId,
                                     final String systemId)
            throws IOException {
        final String resource = DTD_MAP.get(new DTDKey(publicId, systemId));
        if (resource != null) {
            return loadFromResource(resource);
        }

        if (systemId != null) {
            return loadFromSystemId(systemId);
        }

        return null;
    }

    @Nullable
    private InputSource loadFromSystemId(final String systemId) {
        try {
            URI systemIdUrl = new URI(systemId);
            if ("file".equals(systemIdUrl.getScheme())) {
                return loadFromLocalFile(systemIdUrl);
            } else {
                return new InputSource(systemId);
            }
        } catch (Exception e) {
            LOG.warn("Entity lookup failed for system id " + systemId, e);
            return null;
        }
    }

    @Nullable
    private InputSource loadFromLocalFile(final URI systemIdUrl) throws IOException {
        File file = new File(systemIdUrl.getPath());
        if (file.exists()) {
            return sourceFromFile(file.getAbsolutePath());
        }

        String normalisedFilePath = Paths.get(systemIdUrl).normalize().toAbsolutePath().toString();
        String cwd = System.getProperties().getProperty("user.dir");
        if (normalisedFilePath.startsWith(cwd)) {
            String relativePath = normalisedFilePath.substring(cwd.length() + 1);
            final String resolvedFile = configurationLocation.resolveAssociatedFile(relativePath, null, checkstyleClassLoader);
            if (resolvedFile != null) {
                return sourceFromFile(resolvedFile);
            }
        }
        return null;
    }

    @NotNull
    private InputSource sourceFromFile(final String filePath) throws FileNotFoundException {
        return new InputSource(new BufferedInputStream(new FileInputStream(filePath)));
    }

    @Nullable
    private InputSource loadFromResource(final String resource) throws IOException {
        final URL resourceUrl = getClass().getResource(resource);
        if (resourceUrl != null) {
            return new InputSource(resourceUrl.openStream());
        } else {
            LOG.warn("Configured DTD cannot be found: " + resource);
            return null;
        }
    }

    /**
     * A key class for a DTD.
     */
    static class DTDKey {

        private final String publicId;
        private final String systemId;

        /**
         * Create a new key for the given public and system IDs.
         *
         * @param publicId the public ID.
         * @param systemId the system ID.
         */
        DTDKey(@Nullable final String publicId,
               @Nullable final String systemId) {
            this.publicId = publicId;
            this.systemId = systemId;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            DTDKey dtdKey = (DTDKey) o;

            if (!Objects.equals(publicId, dtdKey.publicId)) {
                return false;
            }
            return Objects.equals(systemId, dtdKey.systemId);
        }

        @Override
        public int hashCode() {
            int result = publicId != null ? publicId.hashCode() : 0;
            result = 31 * result + (systemId != null ? systemId.hashCode() : 0);
            return result;
        }
    }

}
