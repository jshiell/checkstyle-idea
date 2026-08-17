package org.infernus.idea.checkstyle.util;

/**
 * Whether Checkstyle has been told to resolve external DTDs and entities.
 * <p>
 * This is Checkstyle's own opt-in, read from the same system property that the Checkstyle CLI and the Gradle
 * plugin use, so that a configuration that works with those also works here. Checkstyle reads the property afresh
 * on every parse, so there is nothing to cache.
 */
public final class ExternalDtdLoad {

    /**
     * Checkstyle's {@code XmlLoader.LoadExternalDtdFeatureProvider.ENABLE_EXTERNAL_DTD_LOAD}. Checkstyle is
     * compile-only for the plugin's own source sets, so the name is repeated rather than referenced.
     */
    public static final String ENABLE_EXTERNAL_DTD_LOAD = "checkstyle.enableExternalDtdLoad";

    private ExternalDtdLoad() {
    }

    public static boolean isEnabled() {
        return Boolean.parseBoolean(System.getProperty(ENABLE_EXTERNAL_DTD_LOAD));
    }
}
