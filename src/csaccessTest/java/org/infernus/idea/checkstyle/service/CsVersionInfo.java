package org.infernus.idea.checkstyle.service;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.infernus.idea.checkstyle.VersionComparator;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;


/**
 * Utility class for unit tests in 'csaccessTest' to query the currently used Checkstyle runtime.
 */
public class CsVersionInfo {
    private static final String PROPS_FILE_NAME = "/checkstyle-idea.properties";
    private static final String BASE_VERSION = readBaseVersion();

    public static final String CSVERSION_SYSPROP_NAME = "org.infernus.idea.checkstyle.version";

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

    public static boolean csVersionIsOneOf(@NotNull final String expectedCsVersion,
                                           @Nullable final String... otherPossibleVersions) {
        final String actualCsVersion = currentCsVersion();
        final List<String> expectedVersions = new ArrayList<>();
        expectedVersions.add(expectedCsVersion);
        if (otherPossibleVersions != null) {
            expectedVersions.addAll(Arrays.asList(otherPossibleVersions));
        }
        return expectedVersions.contains(actualCsVersion);
    }

    public static boolean csVersionIsLessThan(@NotNull final String pCsVersion) {
        return new VersionComparator().compare(currentCsVersion(), pCsVersion) < 0;
    }

    public static boolean csVersionIsGreaterThan(@NotNull final String pCsVersion) {
        return new VersionComparator().compare(currentCsVersion(), pCsVersion) > 0;
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
        return new TypeSafeMatcher<String>() {
            @Override
            protected boolean matchesSafely(String actualCsVersion) {
                return new VersionComparator().compare(actualCsVersion, expectedCsVersion) >= 0;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("is greater than").appendValue(expectedCsVersion);
            }
        };
    }

    public static Matcher<String> isLessThan(@NotNull final String expectedCsVersion) {
        return new TypeSafeMatcher<String>() {
            @Override
            protected boolean matchesSafely(String actualCsVersion) {
                return new VersionComparator().compare(actualCsVersion, expectedCsVersion) < 0;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("is less than").appendValue(expectedCsVersion);
            }
        };
    }
}
