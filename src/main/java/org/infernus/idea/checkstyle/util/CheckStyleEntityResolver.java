package org.infernus.idea.checkstyle.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity resolver for CheckStyle DTDs.
 */
public class CheckStyleEntityResolver implements EntityResolver {

    private static final Log LOG = LogFactory.getLog(CheckStyleEntityResolver.class);

    private static final Map<DTDKey, String> DTD_MAP = new HashMap<>();

    static {
        DTD_MAP.put(new DTDKey(
                        "-//Puppy Crawl//DTD Check Configuration 1.0//EN",
                        "http://www.puppycrawl.com/dtds/configuration_1_0.dtd"),
                "/dtd/configuration_1_0.dtd");
        DTD_MAP.put(new DTDKey(
                        "-//Puppy Crawl//DTD Check Configuration 1.1//EN",
                        "http://www.puppycrawl.com/dtds/configuration_1_1.dtd"),
                "/dtd/configuration_1_1.dtd");
        DTD_MAP.put(new DTDKey(
                        "-//Puppy Crawl//DTD Check Configuration 1.2//EN",
                        "http://www.puppycrawl.com/dtds/configuration_1_2.dtd"),
                "/dtd/configuration_1_2.dtd");
        DTD_MAP.put(new DTDKey(
                        "-//Puppy Crawl//DTD Check Configuration 1.3//EN",
                        "http://www.puppycrawl.com/dtds/configuration_1_3.dtd"),
                "/dtd/configuration_1_3.dtd");

        DTD_MAP.put(new DTDKey("-//Puppy Crawl//DTD Import Control 1.0//EN",
                        "http://www.puppycrawl.com/dtds/import_control_1_0.dtd"),
                "/dtd/import_control_1_0.dtd");
        DTD_MAP.put(new DTDKey("-//Puppy Crawl//DTD Import Control 1.1//EN",
                        "http://www.puppycrawl.com/dtds/import_control_1_1.dtd"),
                "/dtd/import_control_1_1.dtd");

        DTD_MAP.put(new DTDKey("-//Puppy Crawl//DTD Suppressions 1.0//EN",
                        "http://www.puppycrawl.com/dtds/suppressions_1_0.dtd"),
                "/dtd/suppressions_1_0.dtd");
        DTD_MAP.put(new DTDKey("-//Puppy Crawl//DTD Suppressions 1.1//EN",
                        "http://www.puppycrawl.com/dtds/suppressions_1_1.dtd"),
                "/dtd/suppressions_1_0.dtd");

        DTD_MAP.put(new DTDKey("-//Puppy Crawl//DTD Package Names 1.0//EN",
                        "http://www.puppycrawl.com/dtds/packages_1_0.dtd"),
                "/dtd/packages_1_0.dtd");
    }

    private final ConfigurationLocation configurationLocation;

    public CheckStyleEntityResolver(final ConfigurationLocation configurationLocation) {
        this.configurationLocation = configurationLocation;
    }

    @Override
    public InputSource resolveEntity(final String publicId,
                                     final String systemId)
            throws SAXException, IOException {
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
                File file = new File(systemIdUrl.getPath());
                if (file.exists()) {
                    return sourceFromFile(file.getAbsolutePath());
                }

                String normalisedFilePath = Paths.get(systemIdUrl).normalize().toAbsolutePath().toString();
                String cwd = System.getProperties().getProperty("user.dir");
                if (normalisedFilePath.startsWith(cwd)) {
                    String relativePath = normalisedFilePath.substring(cwd.length() + 1);
                    final String resolvedFile = configurationLocation.resolveAssociatedFile(relativePath, null);
                    if (resolvedFile != null) {
                        return sourceFromFile(resolvedFile);
                    }
                }
                return null;
            } else {
                return new InputSource(systemId);
            }
        } catch (Exception e) {
            LOG.warn("Entity lookup failed for system id " + systemId, e);
            return null;
        }
    }

    @NotNull
    private InputSource sourceFromFile(String filePath) throws FileNotFoundException {
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
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DTDKey dtdKey = (DTDKey) o;

            if (publicId != null ? !publicId.equals(dtdKey.publicId) : dtdKey.publicId != null) return false;
            return systemId != null ? systemId.equals(dtdKey.systemId) : dtdKey.systemId == null;
        }

        @Override
        public int hashCode() {
            int result = publicId != null ? publicId.hashCode() : 0;
            result = 31 * result + (systemId != null ? systemId.hashCode() : 0);
            return result;
        }
    }

}
