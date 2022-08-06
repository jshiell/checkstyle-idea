package org.infernus.idea.checkstyle.service;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.infernus.idea.checkstyle.VersionComparator;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;


/**
 * Utility class for unit tests in 'csaccessTest' to query the currently used Checkstyle runtime.
 */
public final class CsVersionInfo {
    private static final String PROPS_FILE_NAME = "/checkstyle-idea.properties";
    private static final String BASE_VERSION = readBaseVersion();

    public static final String CSVERSION_SYSPROP_NAME = "org.infernus.idea.checkstyle.version";

    private CsVersionInfo() {
    }

    @NotNull
    private static String readBaseVersion() {
        String result;
        try (InputStream is = CsVersionInfo.class.getResourceAsStream(PROPS_FILE_NAME)) {
            Properties props = new Properties();
            props.load(is);
            result = props.getProperty("baseVersion");
        } catch (IOException e) {
            throw new CheckStylePluginException("internal error - Failed to read property file: " + PROPS_FILE_NAME, e);
        }
        assertNotNull(result);
        return result;
    }

    @NotNull
    public static String currentCsVersion() {
        final String sysPropValue = System.getProperty(CSVERSION_SYSPROP_NAME);
        if (sysPropValue == null) {
            return BASE_VERSION;
        } else {
            Assert.assertTrue("System property \"" + CSVERSION_SYSPROP_NAME //
                            + "\" does not contain a valid Checkstyle version: " + sysPropValue, //
                    VersionComparator.isValidVersion(System.getProperty(CSVERSION_SYSPROP_NAME)));
            return sysPropValue;
        }
    }

    public static Matcher<String> isGreaterThanOrEqualTo(@NotNull final String expectedCsVersion) {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(final String actualCsVersion) {
                return new VersionComparator().compare(actualCsVersion, expectedCsVersion) >= 0;
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("is greater than").appendValue(expectedCsVersion);
            }
        };
    }

}
