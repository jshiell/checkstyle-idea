package org.infernus.idea.checkstyle.build;

import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Defines an ordering by version number. The Strings are assumed to be in the form {@code "n.m.k"}, where <i>n</i>,
 * <i>m</i>, and <i>k</i> are integer numbers. Strings that do not match this format are sorted alphabetically at the
 * end. <code>null</code> values are sorted at the very end.
 */
public final class VersionComparator implements Comparator<String>, Serializable {

    @Serial
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
        if (pStr1 == null) {
            return pStr2 == null ? 0 : 1;
        }
        if (pStr2 == null) {
            return -1;
        }
        final Matcher matcher1 = PATTERN.matcher(pStr1);
        final Matcher matcher2 = PATTERN.matcher(pStr2);
        if (matcher1.matches() && matcher2.matches()) {
            return Integer.compare(intGroup(matcher1, VE.Major), intGroup(matcher2, VE.Major)) != 0
                    ? Integer.compare(intGroup(matcher1, VE.Major), intGroup(matcher2, VE.Major))
                    : Integer.compare(intGroup(matcher1, VE.Minor), intGroup(matcher2, VE.Minor)) != 0
                    ? Integer.compare(intGroup(matcher1, VE.Minor), intGroup(matcher2, VE.Minor))
                    : Integer.compare(intGroup(matcher1, VE.Micro), intGroup(matcher2, VE.Micro));
        }
        return pStr1.compareTo(pStr2);
    }

    private static int intGroup(final Matcher matcher, final VE group) {
        final String value = matcher.group(group.ordinal());
        return value != null ? Integer.parseInt(value) : 0;
    }
}
