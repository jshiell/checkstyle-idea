package org.infernus.idea.checkstyle.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.infernus.idea.checkstyle.VersionComparator;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;


/**
 * Utility class for unit tests in 'csaccessTest' to query the currently used Checkstyle runtime.
 */
public class CsVersionInfo
{
    private static final String PROPS_FILE_NAME = "/checkstyle-idea.properties";

    public static final String CSVERSION_SYSPROP_NAME = "org.infernus.idea.checkstyle.version";

    private static final String BASE_VERSION = readBaseVersion();


    @NotNull
    private static String readBaseVersion() {
        String result = null;
        try (InputStream is = CsVersionInfo.class.getResourceAsStream(PROPS_FILE_NAME)) {
            Properties props = new Properties();
            props.load(is);
            result = props.getProperty("baseVersion");
        } catch (IOException e) {
            throw new CheckStylePluginException("internal error - Failed to read property file: " + PROPS_FILE_NAME, e);
        }
        Assert.assertNotNull(result);
        return result;
    }


    public static boolean isExactly(@NotNull final String pExpectedCsVersion) {
        return pExpectedCsVersion.equals(getCurrentCsVersion());
    }


    public static boolean isOneOf(@NotNull final String pExpectedCsVersion, @Nullable final String...
            pOtherPossibleVersions) {
        final String actualCsVersion = getCurrentCsVersion();
        final List<String> expectedVersions = new ArrayList<>();
        expectedVersions.add(pExpectedCsVersion);
        if (pOtherPossibleVersions != null) {
            expectedVersions.addAll(Arrays.asList(pOtherPossibleVersions));
        }
        return expectedVersions.contains(actualCsVersion);
    }


    public static boolean isLessThan(@NotNull final String pCsVersion) {
        final String actualCsVersion = getCurrentCsVersion();
        return new VersionComparator().compare(actualCsVersion, pCsVersion) < 0;
    }


    public static boolean isGreaterThan(@NotNull final String pCsVersion) {
        final String actualCsVersion = getCurrentCsVersion();
        return new VersionComparator().compare(actualCsVersion, pCsVersion) > 0;
    }


    @NotNull
    public static String getCurrentCsVersion() {
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
}
