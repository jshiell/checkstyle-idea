package org.infernus.idea.checkstyle.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.infernus.idea.checkstyle.VersionComparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;


/**
 * Utility class for unit tests in 'csaccessTest' to query the currently used Checkstyle runtime.
 */
public class CsVersionInfo
{
    public static final String CSVERSION_SYSPROP_NAME = "org.infernus.idea.checkstyle.version";


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
        Assert.assertTrue("System property \"" + CSVERSION_SYSPROP_NAME //
                + "\" does not contain a valid Checkstyle version: " + System.getProperty(CSVERSION_SYSPROP_NAME),
                VersionComparator.isValidVersion(System.getProperty(CSVERSION_SYSPROP_NAME)));
        return System.getProperty(CSVERSION_SYSPROP_NAME);
    }
}
