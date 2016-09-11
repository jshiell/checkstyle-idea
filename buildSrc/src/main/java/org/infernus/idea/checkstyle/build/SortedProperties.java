package org.infernus.idea.checkstyle.build;

import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * Just so we generate a sorted property file with the classpath infos.
 */
class SortedProperties
        extends Properties {

    @Override
    public synchronized Enumeration<Object> keys() {
        List<Object> keyList = Collections.list(super.keys());
        final Comparator<String> versionComparator = new VersionComparator();
        Collections.sort(keyList, (a, b) -> versionComparator.compare(a.toString(), b.toString()));
        return Collections.enumeration(keyList);
    }
}
