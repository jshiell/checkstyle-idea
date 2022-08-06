package org.infernus.idea.checkstyle;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Defines an ordering by Checkstyle version number. The Strings are assumed to be in the form {@code "n.m.k"}, where
 * <i>n</i>, <i>m</i>, and <i>k</i> are integer numbers. Strings that do not match this format are sorted alphabetically
 * at the end. <code>null</code> values are sorted at the very end.
 */
public final class VersionComparator implements Comparator<String>, Serializable {

    private static final long serialVersionUID = 0L;

    private static final Pattern PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    /**
     * Matching groups of the above {@link #PATTERN}.
     */
    private enum VE {
        @SuppressWarnings("unused")
        All,
        Major,
        Minor,
        Micro
    }


    @Override
    public int compare(final String pStr1, final String pStr2) {
        int result = 0;
        if (pStr1 == null) {
            if (pStr2 != null) {
                result = 1;
            }
        } else {
            if (pStr2 == null) {
                result = -1;
            } else {
                Matcher matcher1 = PATTERN.matcher(pStr1);
                Matcher matcher2 = PATTERN.matcher(pStr2);
                if (matcher1.matches() && matcher2.matches()) {
                    final int major1 = Integer.parseInt(matcher1.group(VE.Major.ordinal()));
                    final int major2 = Integer.parseInt(matcher2.group(VE.Major.ordinal()));
                    final int minor1 = Integer.parseInt(matcher1.group(VE.Minor.ordinal()));
                    final int minor2 = Integer.parseInt(matcher2.group(VE.Minor.ordinal()));
                    final int micro1 = matcher1.group(VE.Micro.ordinal()) != null ? Integer.parseInt(
                            matcher1.group(VE.Micro.ordinal())) : 0;
                    final int micro2 = matcher2.group(VE.Micro.ordinal()) != null ? Integer.parseInt(
                            matcher2.group(VE.Micro.ordinal())) : 0;

                    if (major1 > major2) {
                        result = 1;
                    } else if (major1 < major2) {
                        result = -1;
                    } else {
                        if (minor1 > minor2) {
                            result = 1;
                        } else if (minor1 < minor2) {
                            result = -1;
                        } else {
                            if (micro1 > micro2) {
                                result = 1;
                            } else if (micro1 < micro2) {
                                result = -1;
                            }
                        }
                    }
                } else {
                    result = pStr1.compareTo(pStr2);
                }
            }
        }
        return result;
    }


    public static boolean isValidVersion(@Nullable final String pCsVersion) {
        return pCsVersion != null && PATTERN.matcher(pCsVersion).matches();
    }
}
