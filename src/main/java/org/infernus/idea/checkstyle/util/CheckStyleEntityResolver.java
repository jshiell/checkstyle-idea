package org.infernus.idea.checkstyle.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity resolver for CheckStyle DTDs.
 */
public class CheckStyleEntityResolver implements EntityResolver {

    /**
     * Logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(
            CheckStyleEntityResolver.class);

    private static final Map<DTDKey, String> DTD_MAP
            = new HashMap<>();

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

    public InputSource resolveEntity(final String publicId,
                                     final String systemId)
            throws SAXException, IOException {
        final String resource = DTD_MAP.get(new DTDKey(publicId, systemId));

        if (resource != null) {
            final URL resourceUrl = getClass().getResource(resource);
            if (resourceUrl != null) {
                return new InputSource(resourceUrl.openStream());
            } else {
                LOG.warn("Configured DTD cannot be found: " + resource);
            }
        }

        return null;
    }

    /**
     * A key class for a DTD.
     */
    protected static class DTDKey {

        private final String publicId;
        private final String systemId;

        /**
         * Create a new key for the given public and system IDs.
         *
         * @param publicId the public ID.
         * @param systemId the system ID.
         */
        public DTDKey(@NotNull final String publicId,
                      @NotNull final String systemId) {
            this.publicId = publicId;
            this.systemId = systemId;
        }

        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final DTDKey that = (DTDKey) o;

            if (!publicId.equals(that.publicId)) {
                return false;
            }
            if (!systemId.equals(that.systemId)) {
                return false;
            }

            return true;
        }

        public int hashCode() {
            return 31 * publicId.hashCode() + systemId.hashCode();
        }
    }

}
