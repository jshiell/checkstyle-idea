package org.infernus.idea.checkstyle.util;

public final class ObjectUtils {

    private ObjectUtils() {
    }

    // ideally we'd use common-lang here, but I've learnt my lesson on relying on the IDEA CP

    public static boolean equals(final Object obj1, final Object obj2) {
        if (obj1 == obj2) {
            return true;

        } else if (obj1 == null || obj2 == null) {
            return false;
        }

        return obj1.equals(obj2);
    }

    public static <T extends Comparable<T>> int compare(final T obj1, final T obj2) {
        if (obj1 == null && obj2 == null) {
            return 0;
        } else if (obj1 == null) {
            return -1;
        } else if (obj2 == null) {
            return 1;
        }
        return obj1.compareTo(obj2);
    }
}
