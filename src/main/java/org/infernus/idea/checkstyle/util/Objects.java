package org.infernus.idea.checkstyle.util;

public final class Objects {

    private Objects() {
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
